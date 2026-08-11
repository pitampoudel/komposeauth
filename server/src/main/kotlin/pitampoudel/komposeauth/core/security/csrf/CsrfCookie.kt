package pitampoudel.komposeauth.core.security.csrf

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.DefaultCsrfToken
import org.springframework.stereotype.Component
import org.springframework.web.util.WebUtils
import pitampoudel.komposeauth.app_config.service.AppConfigService
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

private val cookieDomainLog = LoggerFactory.getLogger("pitampoudel.komposeauth.core.security.csrf")
private val warnedAboutRpId = AtomicBoolean(false)

/**
 * The domain the auth cookies are scoped to, or null for a host-only cookie.
 *
 * Returned bare, with no leading dot. The dot is an RFC 2109 habit that RFC 6265 dropped: a
 * `Domain` attribute already means "this host and every subdomain of it", and §4.1.2.3 has the
 * browser strip a leading dot before doing anything with the value. So it buys nothing — and it is
 * not merely redundant, it is invalid. Tomcat's `Rfc6265CookieProcessor` validates the grammar
 * strictly and rejects a domain whose first label is empty, so `.example.com` throws
 * `IllegalArgumentException` the moment anything writes the cookie through `response.addCookie`.
 * That happens on every successful sign-in, inside `CsrfAuthenticationStrategy`, which is a filter —
 * hence a whitelabel 500 on the OAuth callback with no handler able to intervene.
 *
 * `rpId` is unset until an operator configures it, and the previous `"." + rpId()` produced the
 * literal string `.null` in that case — a domain no browser accepts, so the cookie was dropped and
 * cookie sign-in silently did nothing. A host-only cookie is the right fallback: it works, it is
 * simply not shared with subdomains.
 *
 * Everything else here is about not letting a typo take the site down. `rpId` is free text on a
 * configuration form, and `ResponseCookie` *validates* what it is given — a value pasted as a URL,
 * or written with the leading dot a cookie domain conventionally has, yields `.https://host` or
 * `..host` and throws. Thrown while writing a cookie, that lands inside a servlet filter, where no
 * exception handler can reach it, and every page it touches becomes a whitelabel 500 — including
 * the configuration page where the mistake could have been corrected. So normalise what can be
 * salvaged, refuse the rest by falling back to a host-only cookie, and say so once in the log.
 */
fun authCookieDomain(appConfigService: AppConfigService): String? {
    val configured = appConfigService.rpId()?.trim()?.takeIf { it.isNotBlank() } ?: return null

    val host = configured
        .substringAfter("://")      // pasted as a URL
        .substringBefore('/')       // trailing path
        .substringBefore(':')       // port
        .trim('.')                  // the conventional leading dot, which is not put back
        .lowercase()

    if (host.isEmpty() || !VALID_DOMAIN.matches(host)) {
        if (warnedAboutRpId.compareAndSet(false, true)) {
            cookieDomainLog.warn(
                "Relying party ID '{}' is not usable as a cookie domain, so sign-in cookies will " +
                        "not be shared with subdomains. Set it to a bare hostname such as " +
                        "'example.com' — no scheme, port or path.",
                configured
            )
        }
        return null
    }
    return host
}

/** Letters, digits, dots and hyphens, with no empty or hyphen-edged label — what a cookie may name. */
private val VALID_DOMAIN = Regex("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*")

/**
 * Issues the CSRF cookie with the same reach as the access-token cookie.
 *
 * [org.springframework.security.web.csrf.CookieCsrfTokenRepository] cannot express this: it is
 * final, and its cookie customizer never sees the request, so it cannot decide `Secure` and
 * `SameSite` per request the way the access-token cookie does.
 *
 * Matching that cookie's attributes is the whole point. The access token is deliberately
 * `SameSite=None` and scoped to `rpId`, so it reaches the server from browser apps on sibling
 * origins. A CSRF cookie left host-only and `SameSite=Lax` — the framework default — is neither
 * sent on those requests nor readable by those apps, which would leave every cross-origin
 * cookie-authenticated write permanently rejected with no way to obtain a token.
 *
 * Widening the cookie does not weaken the protection. Double-submit relies on an attacker being
 * unable to *read* the value, and the same-origin policy still forbids that: a page on an unrelated
 * domain cannot read a cookie scoped to `rpId` no matter which requests it rides along on.
 */
@Component
class CrossOriginCsrfTokenRepository(
    private val appConfigService: AppConfigService
) : CsrfTokenRepository {

    private val secureRandom = SecureRandom()

    override fun generateToken(request: HttpServletRequest): CsrfToken =
        DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, newTokenValue())

    override fun saveToken(
        token: CsrfToken?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val value = token?.token.orEmpty()
        val secure = request.isSecure
        val cookie = ResponseCookie.from(COOKIE_NAME, value)
            // Readable by the browser app that has to echo it back in a header.
            .httpOnly(false)
            .secure(secure)
            .path("/")
            // `SameSite=None` is only honoured on a Secure cookie, so plain-HTTP local development
            // falls back to Lax — where it is also all that is needed, there being no cross-site
            // caller in that setup.
            .sameSite(if (secure) "None" else "Lax")
            .domain(authCookieDomain(appConfigService))
            .maxAge(if (token == null) 0 else -1)
            .build()
        response.addCookie(cookie.toServletCookie())

        // Lets loadToken report "no token" for the rest of this request after one has been cleared,
        // so the framework regenerates instead of handing back the value it just deleted.
        if (value.isEmpty()) {
            request.setAttribute(REMOVED_ATTRIBUTE, true)
        } else {
            request.removeAttribute(REMOVED_ATTRIBUTE)
        }
    }

    override fun loadToken(request: HttpServletRequest): CsrfToken? {
        if (request.getAttribute(REMOVED_ATTRIBUTE) == true) return null
        val value = WebUtils.getCookie(request, COOKIE_NAME)?.value
        if (value.isNullOrEmpty()) return null
        return DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, value)
    }

    /**
     * Hands the cookie to the container as a cookie rather than a pre-rendered `Set-Cookie` string.
     *
     * Servlet 6 carries `SameSite` as a plain attribute, so nothing is lost by doing it this way,
     * and the container is left to serialise it — which also means anything reading the response
     * back, test harnesses included, sees a cookie instead of a line of text it has to parse.
     */
    private fun ResponseCookie.toServletCookie(): Cookie =
        Cookie(name, value).also { servletCookie ->
            servletCookie.isHttpOnly = isHttpOnly
            servletCookie.secure = isSecure
            servletCookie.path = path
            servletCookie.maxAge = maxAge.seconds.toInt()
            domain?.takeIf { it.isNotBlank() }?.let { servletCookie.domain = it }
            sameSite?.takeIf { it.isNotBlank() }?.let { servletCookie.setAttribute("SameSite", it) }
        }

    private fun newTokenValue(): String {
        val bytes = ByteArray(TOKEN_BYTES).also { secureRandom.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        const val COOKIE_NAME = "XSRF-TOKEN"
        const val HEADER_NAME = "X-XSRF-TOKEN"
        const val PARAMETER_NAME = "_csrf"
        private const val TOKEN_BYTES = 32
        private val REMOVED_ATTRIBUTE = "${CrossOriginCsrfTokenRepository::class.java.name}.REMOVED"
    }
}
