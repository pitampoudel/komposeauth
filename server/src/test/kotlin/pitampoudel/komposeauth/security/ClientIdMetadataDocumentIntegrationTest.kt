package pitampoudel.komposeauth.security

import jakarta.servlet.http.Cookie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.oauth_clients.cimd.ClientIdMetadataDocuments
import pitampoudel.komposeauth.oauth_clients.cimd.ClientMetadataFetcher
import pitampoudel.komposeauth.oauth_clients.cimd.InvalidClientMetadata
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * MCP clients such as Claude identify themselves with a Client ID Metadata Document: their
 * client id is an HTTPS URL serving their metadata, so they sign users in without being
 * registered here first.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class, ClientIdMetadataDocumentIntegrationTest.Documents::class)
@AutoConfigureMockMvc
class ClientIdMetadataDocumentIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    class Documents {
        /** The documents clients publish, by URL, instead of fetching them from the internet. */
        @Bean
        @Primary
        fun testClientMetadataFetcher(): ClientMetadataFetcher = ClientMetadataFetcher { url ->
            PUBLISHED[url.toString()] ?: throw InvalidClientMetadata("$url answered 404")
        }
    }

    companion object {
        const val CLIENT = "https://assistant.example/oauth/client-metadata"
        const val IMPOSTOR = "https://impostor.example/oauth/client-metadata"
        const val REDIRECT = "https://assistant.example/api/mcp/auth_callback"

        val PUBLISHED = mapOf(
            CLIENT to """
                {"client_id": "$CLIENT", "client_name": "Assistant", "redirect_uris": ["$REDIRECT"],
                 "grant_types": ["authorization_code", "refresh_token"], "response_types": ["code"],
                 "token_endpoint_auth_method": "none"}
            """,
            // Claims to be someone else's client
            IMPOSTOR to """{"client_id": "$CLIENT", "redirect_uris": ["$REDIRECT"], "token_endpoint_auth_method": "none"}""",
        )
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var json: Json
    @Autowired private lateinit var documents: ClientIdMetadataDocuments

    private val password = "Password1"
    private val codeVerifier = "c".repeat(43)
    private var sessionCookie: Cookie? = null

    private fun remember(result: MvcResult) {
        result.response.getCookie("SESSION")?.let {
            sessionCookie = if (it.maxAge == 0 || it.value.isNullOrEmpty()) null else it
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    private fun authorizeUrl(clientId: String, redirectUri: String = REDIRECT): String {
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        )
        return "/oauth2/authorize?response_type=code&client_id=${encode(clientId)}&redirect_uri=${encode(redirectUri)}" +
            "&scope=${encode("openid profile email")}&state=s1&code_challenge=$challenge&code_challenge_method=S256"
    }

    /** Signs in through the login page; returns what /oauth2/authorize finally answers. */
    private fun signIn(url: String, email: String): MvcResult {
        var next = url
        repeat(12) {
            val result = mockMvc.get(URI(next)) {
                sessionCookie?.let { cookie(it) }
                accept = MediaType.TEXT_HTML
            }.andReturn()
            remember(result)
            val location = result.response.redirectedUrl
            if (location == null || location.startsWith(REDIRECT)) return result
            next = if (location.contains("login")) {
                mockMvc.post("/session-login") {
                    sessionCookie?.let { cookie(it) }
                    param("username", email)
                    param("password", password)
                }.andReturn().also { remember(it) }.response.redirectedUrl!!
            } else location
        }
        error("the sign-in never finished")
    }

    private fun token(vararg params: Pair<String, String>): Map<String, String> {
        val result = mockMvc.post("/oauth2/token") {
            params.forEach { (name, value) -> param(name, value) }
        }.andReturn()
        assertEquals(200, result.response.status, "${params.first().second}: ${result.response.contentAsString}")
        return json.parseToJsonElement(result.response.contentAsString).jsonObject
            .mapValues { it.value.jsonPrimitive.content }
    }

    @Test
    fun `the server tells clients they may use a metadata document`() {
        for (path in listOf("/.well-known/oauth-authorization-server", "/.well-known/openid-configuration")) {
            val metadata = json.parseToJsonElement(mockMvc.get(path).andReturn().response.contentAsString).jsonObject
            assertTrue(metadata["client_id_metadata_document_supported"]!!.jsonPrimitive.boolean, path)
            val methods = (metadata["token_endpoint_auth_methods_supported"] as JsonArray).map { it.jsonPrimitive.content }
            assertTrue("none" in methods, "$path: $methods")
        }
    }

    @Test
    fun `a client from a metadata document signs a user in after they consent`() {
        val email = "cimd-user@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

        // Unlike a registered client, it always asks the user first, naming itself by its host
        val consent = signIn(authorizeUrl(CLIENT), email)
        val page = consent.response.contentAsString
        assertEquals(200, consent.response.status, "${consent.response.redirectedUrl} $page")
        assertTrue(page.contains("assistant.example"), page)
        val state = assertNotNull(Regex("""name="state" value="([^"]+)"""").find(page)?.groupValues?.get(1), page)

        val approved = mockMvc.post("/oauth2/authorize") {
            sessionCookie?.let { cookie(it) }
            param("client_id", CLIENT)
            param("state", state)
            param("scope", "profile")
            param("scope", "email")
        }.andReturn()
        val location = assertNotNull(approved.response.redirectedUrl, "${approved.response.status} ${approved.response.errorMessage} ${approved.response.contentAsString}")
        assertTrue(location.startsWith("$REDIRECT?code="), location)
        val code = location.substringAfter("?code=").substringBefore("&")

        val first = token(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to REDIRECT,
            "client_id" to CLIENT,
            "code_verifier" to codeVerifier
        )
        assertNotNull(first["access_token"])
        val refreshToken = assertNotNull(first["refresh_token"], "no refresh token in $first")

        // A public client's refresh token is rotated
        val refreshed = token("grant_type" to "refresh_token", "refresh_token" to refreshToken, "client_id" to CLIENT)
        assertNotEquals(refreshToken, refreshed["refresh_token"])
        val reuse = mockMvc.post("/oauth2/token") {
            param("grant_type", "refresh_token")
            param("refresh_token", refreshToken)
            param("client_id", CLIENT)
        }.andReturn()
        assertEquals(400, reuse.response.status)
    }

    @Test
    fun `a document that isn't the client's own, or a redirect it doesn't list, is refused`() {
        val email = "cimd-refused@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email, password)

        for (url in listOf(authorizeUrl(IMPOSTOR), authorizeUrl(CLIENT, "https://evil.example/callback"), authorizeUrl("https://missing.example/client"))) {
            val result = signIn(url, email)
            assertEquals(400, result.response.status, url)
            assertFalse(result.response.redirectedUrl.orEmpty().contains("code="), url)
        }
    }

    @Test
    fun `only HTTPS URLs with a path are metadata documents`() {
        assertTrue(documents.isDocumentUrl("https://claude.ai/oauth/claude-code-client-metadata"))
        for (id in listOf("6abd2aac48ce5081641b4ac1", "http://example.com/client", "https://example.com", "https://example.com/",
            "https://user@example.com/client", "https://example.com/a/../client", "https://example.com/client#x")) {
            assertFalse(documents.isDocumentUrl(id), id)
        }
    }
}
