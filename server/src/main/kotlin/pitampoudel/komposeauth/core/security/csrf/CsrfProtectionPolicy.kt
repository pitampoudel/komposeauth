package pitampoudel.komposeauth.core.security.csrf

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.security.PublicEndpoints

/**
 * Which requests must carry a CSRF token.
 *
 * The rule is about *shape*, not about a list of paths. A list has to be extended by hand for every
 * endpoint, and what it costs when someone forgets is not a hole — it is an outage: the endpoint
 * demands a token the caller has no way to know it needs, and answers 403 to everyone forever. That
 * is what happened to the whole JSON API. This server's own browser client authenticates with the
 * access-token cookie and never fetches a token, so `/verify-otp`, `/update-profile`, the three
 * KYC submissions, `/webauthn/register`, `/organizations` and `/logout` were all refused outright,
 * and naming them one at a time would only have waited for the next endpoint to be written.
 *
 * So ask instead what CSRF is actually for: a request a *cross-site page* can cause a browser to
 * send with the victim's ambient credentials attached. That is a much narrower set than "everything
 * that changes state", and the boundary is drawn by the browser rather than by us.
 *
 * An HTML form may only submit `application/x-www-form-urlencoded`, `multipart/form-data` or
 * `text/plain` — the enctypes the HTML spec allows — and those are exactly the content types CORS
 * treats as safelisted, so a form needs no preflight and can be aimed anywhere. Any other content
 * type can only come from script, and script setting one turns the request into a preflighted CORS
 * request, which fails for every origin not on the configured allow-list. `application/json`, which
 * is what every endpoint in this application's API accepts and what its clients send, is therefore
 * unreachable from a hostile page: the preflight has to pass first, and passing it means the
 * operator put that origin on the list themselves.
 *
 * This is not a new argument here. It is the one already written down for `/login`, which has been
 * CSRF-exempt on precisely these grounds and is asserted in `CsrfProtectionIntegrationTest`. What
 * changes is that it is applied as a rule rather than re-derived for one endpoint at a time.
 *
 * What still needs a token, and gets one from the page it was served on:
 *
 *  - `/admin/config`, the one `@ModelAttribute` endpoint in the server — a real form post, carrying
 *    every secret this server holds, and the single most worthwhile thing to forge.
 *  - `/session-login`, which establishes the session.
 *  - the console's own bodiless writes (role grant and revoke, logout), which send no content type
 *    at all and so are treated as form-shaped.
 *
 * Absent or unparseable content types are counted as form-shaped deliberately: the question is
 * whether a form *could* have produced the request, and when we cannot tell, the answer that costs
 * nothing is the careful one. Everything that legitimately sends no content type here is a
 * server-rendered page's own call, which has the token to hand.
 */
object CsrfProtectionPolicy {

    /** Methods that are not supposed to change anything, so nothing is forged by making them. */
    private val SAFE_METHODS = setOf("GET", "HEAD", "TRACE", "OPTIONS")

    /** The only enctypes an HTML form can submit, and so the only ones reachable without CORS. */
    private val FORM_SUBMITTABLE = listOf(
        MediaType.APPLICATION_FORM_URLENCODED,
        MediaType.MULTIPART_FORM_DATA,
        MediaType.TEXT_PLAIN
    )

    fun matcher(): RequestMatcher = AndRequestMatcher(
        stateChanging(),
        formShaped(),
        NegatedRequestMatcher(
            OrRequestMatcher(
                PublicEndpoints.csrfExemptRequestMatcher(),
                headerOnlyBearerRequest()
            )
        )
    )

    private fun stateChanging(): RequestMatcher =
        RequestMatcher { request -> request.method !in SAFE_METHODS }

    /** Whether a cross-site HTML form could have produced this request. See the note above. */
    private fun formShaped(): RequestMatcher = RequestMatcher { request ->
        val declared = request.contentType?.takeIf { it.isNotBlank() } ?: return@RequestMatcher true
        val mediaType = try {
            MediaType.parseMediaType(declared)
        } catch (_: InvalidMediaTypeException) {
            return@RequestMatcher true
        }
        FORM_SUBMITTABLE.any { it.includes(mediaType) }
    }

    /**
     * A request that carries a bearer token in the `Authorization` header and no session or
     * access-token cookie cannot be forged cross-site: the browser will not attach that header on
     * its own. Native and server-to-server clients authenticate this way, so exempting them keeps
     * CSRF protection focused on the cookie-authenticated browser surface where it actually applies.
     */
    private fun headerOnlyBearerRequest(): RequestMatcher = RequestMatcher { request ->
        val hasBearerHeader = request.getHeader("Authorization")
            ?.startsWith("Bearer ", ignoreCase = true) == true
        hasBearerHeader && !hasAmbientCredential(request)
    }

    private fun hasAmbientCredential(request: HttpServletRequest): Boolean =
        request.cookies?.any {
            it.name == ACCESS_TOKEN_COOKIE_NAME || it.name == "JSESSIONID" || it.name == "SESSION"
        } == true
}
