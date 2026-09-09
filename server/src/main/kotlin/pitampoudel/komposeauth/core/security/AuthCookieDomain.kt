package pitampoudel.komposeauth.core.security

import org.slf4j.LoggerFactory
import pitampoudel.komposeauth.app_config.service.AppConfigService
import java.util.concurrent.atomic.AtomicBoolean

private val cookieDomainLog = LoggerFactory.getLogger("pitampoudel.komposeauth.core.security")
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
