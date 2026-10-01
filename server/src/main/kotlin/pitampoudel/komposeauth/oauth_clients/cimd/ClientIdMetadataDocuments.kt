package pitampoudel.komposeauth.oauth_clients.cimd

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InvalidClientMetadata(message: String) : RuntimeException(message)

/** Fetches a client's metadata document as text; tests supply their own documents. */
fun interface ClientMetadataFetcher {
    fun fetch(url: URI): String
}

/**
 * Fetches over HTTPS, refusing addresses on private networks (so a client id can't make the server
 * call into its own network), redirects, slow servers and large documents.
 */
@Component
class HttpClientMetadataFetcher : ClientMetadataFetcher {
    private val http = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    override fun fetch(url: URI): String {
        val addresses = try {
            InetAddress.getAllByName(url.host)
        } catch (e: IOException) {
            throw InvalidClientMetadata("can't resolve ${url.host}")
        }
        if (addresses.any { it.isInternal() }) throw InvalidClientMetadata("${url.host} is on a private network")
        val request = HttpRequest.newBuilder(url).timeout(TIMEOUT).header("Accept", "application/json").GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { body ->
            if (response.statusCode() != 200) throw InvalidClientMetadata("$url answered ${response.statusCode()}")
            val bytes = body.readNBytes(MAX_BYTES + 1)
            if (bytes.size > MAX_BYTES) throw InvalidClientMetadata("$url is larger than $MAX_BYTES bytes")
            return bytes.toString(Charsets.UTF_8)
        }
    }

    private fun InetAddress.isInternal(): Boolean =
        isLoopbackAddress || isAnyLocalAddress || isLinkLocalAddress || isSiteLocalAddress || isMulticastAddress ||
            (this is Inet6Address && (address[0].toInt() and 0xfe) == 0xfc) // fc00::/7, unique local

    companion object {
        private val TIMEOUT: Duration = Duration.ofSeconds(5)
        private const val MAX_BYTES = 16 * 1024
    }
}

/**
 * OAuth clients identified by a Client ID Metadata Document
 * (draft-ietf-oauth-client-id-metadata-document, which the MCP authorization spec uses): the
 * client id is an HTTPS URL serving the client's metadata, so clients such as Claude can sign
 * users in without being registered here first.
 *
 * Such a client is always public (PKCE, no secret), may only use the redirect URIs its own document
 * lists, gets the basic sign-in scopes, and always asks the user's consent, naming the client by
 * its URL's host, since anyone can publish a document.
 */
@Component
class ClientIdMetadataDocuments(private val fetcher: ClientMetadataFetcher) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, Cached>()

    private class Cached(val client: RegisteredClient?, val until: Instant)

    /** Whether a client id is a metadata document URL: HTTPS, with a path, without credentials, a fragment or dot segments. */
    fun isDocumentUrl(clientId: String): Boolean {
        if (!clientId.startsWith("https://") || clientId.length > MAX_URL_LENGTH) return false
        val uri = runCatching { URI(clientId) }.getOrNull() ?: return false
        val path = uri.rawPath.orEmpty()
        return uri.host != null && uri.rawUserInfo == null && uri.rawFragment == null &&
            path.length > 1 && path.split('/').none { it == "." || it == ".." }
    }

    /** The client a metadata document describes, or null when it can't be fetched or isn't valid. */
    fun find(clientId: String): RegisteredClient? {
        if (!isDocumentUrl(clientId)) return null
        val now = Instant.now()
        cache[clientId]?.takeIf { it.until.isAfter(now) }?.let { return it.client }
        val client = try {
            toClient(clientId, json.parseToJsonElement(fetcher.fetch(URI(clientId))).jsonObject)
        } catch (e: Exception) {
            log.warn("Client metadata document {} refused: {}", clientId, e.message)
            null
        }
        if (cache.size >= MAX_CACHED) cache.clear()
        cache[clientId] = Cached(client, now.plus(if (client == null) FAILURE_TTL else CACHE_TTL))
        return client
    }

    private fun toClient(clientId: String, doc: JsonObject): RegisteredClient {
        if (doc.string("client_id") != clientId) throw InvalidClientMetadata("its client_id isn't its own URL")
        val authMethod = doc.string("token_endpoint_auth_method") ?: "none"
        if (authMethod != "none") throw InvalidClientMetadata("only public clients (token_endpoint_auth_method none) are supported")

        val redirectUris = doc.strings("redirect_uris")
        if (redirectUris.isEmpty()) throw InvalidClientMetadata("it lists no redirect_uris")
        redirectUris.forEach { if (!allowedRedirect(it)) throw InvalidClientMetadata("redirect URI $it isn't HTTPS or loopback") }

        val grants = doc.strings("grant_types").ifEmpty { listOf("authorization_code") }
        if ("authorization_code" !in grants) throw InvalidClientMetadata("it doesn't use the authorization_code grant")

        return RegisteredClient.withId(clientId)
            .clientId(clientId)
            .clientName(URI(clientId).host)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .apply { if ("refresh_token" in grants) authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN) }
            .redirectUris { it.addAll(redirectUris) }
            .scopes { it.addAll(SCOPES) }
            .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build())
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofHours(1))
                    .refreshTokenTimeToLive(Duration.ofDays(30))
                    // A public client's refresh token is rotated: each use returns a new one (OAuth 2.1)
                    .reuseRefreshTokens(false)
                    .build()
            )
            .build()
    }

    private fun allowedRedirect(uri: String): Boolean {
        val parsed = runCatching { URI(uri) }.getOrNull() ?: return false
        if (parsed.rawFragment != null || parsed.host == null) return false
        return parsed.scheme == "https" ||
            (parsed.scheme == "http" && parsed.host in setOf("127.0.0.1", "[::1]", "localhost"))
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }

    companion object {
        /** What a client from a metadata document may ask for: signing the user in, with their profile and email. */
        val SCOPES = setOf("openid", "profile", "email")
        private val CACHE_TTL: Duration = Duration.ofMinutes(10)
        private val FAILURE_TTL: Duration = Duration.ofMinutes(1)
        private const val MAX_CACHED = 1000
        private const val MAX_URL_LENGTH = 2000
    }
}
