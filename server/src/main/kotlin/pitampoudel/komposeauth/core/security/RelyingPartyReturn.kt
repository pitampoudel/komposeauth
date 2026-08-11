package pitampoudel.komposeauth.core.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Component
import org.springframework.web.util.WebUtils
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * The way back to the application a visitor was sent here from.
 *
 * Somebody who arrives from a relying party is in the middle of that application's sign-in, not
 * visiting this one. Every way out of here should therefore end at their redirect URI — with a code
 * when they sign in, with an OAuth error when they do not — and never on a page of this server.
 * They did between those two: a visitor whose sign-in worked but whose saved authorization request
 * had gone was handed to the success handler's default target, `/`, which is a REST endpoint. They
 * ended a successful sign-in looking at their own profile as raw JSON, on a host they had never
 * chosen to visit, with nothing to click.
 *
 * That saved request lives in the session, which is exactly what goes missing in the cases worth
 * recovering from — a session dropped between here and the provider, an expired one, a callback
 * replayed from a stale tab. So the authorization request is remembered a second time, in a cookie
 * of its own, which outlives the session it was made in.
 *
 * Only the query string is kept. The path it is replayed against is this server's own authorization
 * endpoint, a constant, so nothing a cookie holds can name where the visitor gets sent — and the
 * endpoint validates `client_id` and `redirect_uri` against the client registry as it always did.
 * The error path checks them here for the same reason: an error is only ever returned to a URI the
 * client has registered, so a cookie somebody has rewritten cannot turn this into an open redirect.
 */
@Component
class RelyingPartyReturn(
    private val registeredClientRepository: RegisteredClientRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Called when an authorization request is interrupted to sign the visitor in. */
    fun remember(request: HttpServletRequest, response: HttpServletResponse) {
        if (request.requestURI != request.contextPath + AUTHORIZATION_ENDPOINT) return
        val query = request.queryString?.takeIf { it.isNotBlank() } ?: return
        write(request, response, URLEncoder.encode(query, StandardCharsets.UTF_8), TTL_SECONDS)
    }

    /**
     * The authorization request to replay, if one is remembered. Consumed: a sign-in that does not
     * resume it should not have it waiting the next time round.
     */
    fun resume(request: HttpServletRequest, response: HttpServletResponse): String? {
        val query = peek(request) ?: return null
        clear(request, response)
        return "$AUTHORIZATION_ENDPOINT?$query"
    }

    /**
     * Where to send a visitor whose sign-in ended without a code: the relying party's own redirect
     * URI, carrying [error] and the `state` it started with, per RFC 6749 §4.1.2.1.
     *
     * Null when there is nowhere safe to send them — no remembered request, an unknown client, or a
     * redirect URI that client has not registered. The caller falls back to the login page then,
     * which is this server's page but is at least a page, and one they can sign in from. The record
     * is left alone in that case, so a visitor who does sign in from there is still returned to the
     * application afterwards.
     */
    fun errorRedirect(
        request: HttpServletRequest,
        response: HttpServletResponse,
        error: String
    ): String? {
        val parameters = parseQuery(peek(request) ?: return null)

        val clientId = parameters["client_id"] ?: return null
        val client = registeredClientRepository.findByClientId(clientId) ?: run {
            log.warn("An interrupted authorization request names a client that no longer exists")
            return null
        }

        val requested = parameters["redirect_uri"]
        val redirectUri = when (requested) {
            // Absent is allowed when the client registered exactly one, which is then unambiguous —
            // the same reading the authorization endpoint itself applies.
            null -> client.redirectUris.singleOrNull()
            else -> requested.takeIf { it in client.redirectUris }
        } ?: run {
            log.warn("Refusing to return a visitor to a URI client '{}' has not registered", clientId)
            return null
        }

        clear(request, response)
        val separator = if (redirectUri.contains('?')) "&" else "?"
        return buildString {
            append(redirectUri).append(separator)
            append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8))
            parameters["state"]?.let {
                append("&state=").append(URLEncoder.encode(it, StandardCharsets.UTF_8))
            }
        }
    }

    /** For a sign-in that resumed the authorization request from the session instead. */
    fun clear(request: HttpServletRequest, response: HttpServletResponse) {
        if (WebUtils.getCookie(request, COOKIE_NAME) == null) return
        write(request, response, "", 0)
    }

    private fun peek(request: HttpServletRequest): String? =
        WebUtils.getCookie(request, COOKIE_NAME)?.value
            ?.takeIf { it.isNotBlank() }
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }

    private fun write(
        request: HttpServletRequest,
        response: HttpServletResponse,
        value: String,
        maxAge: Long
    ) {
        val cookie = ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(request.isSecure)
            .path("/")
            // The visitor comes back from the provider through a top-level navigation from another
            // site, which `Lax` is sent on and which is the only crossing this cookie has to make.
            // Host-only for the same reason: nothing on a sibling origin reads it.
            .sameSite("Lax")
            .maxAge(maxAge)
            .build()
        response.addHeader("Set-Cookie", cookie.toString())
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&").mapNotNull { pair ->
            val separator = pair.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8)
            val value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8)
            name to value
        }.toMap()

    companion object {
        const val AUTHORIZATION_ENDPOINT = "/oauth2/authorize"

        private const val COOKIE_NAME = "authorize_request"

        /**
         * Long enough to sign in through a provider without hurrying, short enough that an
         * abandoned sign-in does not send the next visit somewhere it did not ask to go.
         */
        private const val TTL_SECONDS = 15L * 60
    }
}
