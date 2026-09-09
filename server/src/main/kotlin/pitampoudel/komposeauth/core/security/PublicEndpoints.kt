package pitampoudel.komposeauth.core.security

import org.springframework.http.HttpMethod
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.ApiEndpoints.THIRD_FACTOR_KYC


object PublicEndpoints {

    /** Fully public paths: authentication is never attempted; an invalid token is ignored. */
    val purelyPublicPatterns: List<String> = listOf(
        "/css/**",
        "/js/**",
        "/img/**",
        "/lib/**",
        "/favicon.ico",
        "/assets/**",
        "/session-login",
        "/oauth2/jwks",
        // Handing out a CSRF token requires no authority, and a browser app needs one before it can
        // make its first authenticated write.
        "/csrf",
        "/${ApiEndpoints.LOGIN}",
        "/${ApiEndpoints.LOGOUT}",
        "/signup",
        "/${ApiEndpoints.LOGIN_OPTIONS}",
        "/${ApiEndpoints.VERIFY_EMAIL}",
        "/${ApiEndpoints.RESET_PASSWORD}",
        "/reset-password",
        "/countries.json",
        "/.well-known/**",
        "/setup",
        // Container health probes. The platform runs these before anything has signed in and with no
        // credentials to offer, so behind authentication they answer 401 and the probe reads that as
        // a dead instance. Only `health` is exposed over HTTP (see management.endpoints in
        // application.yml) and it is configured to report a bare status, so this discloses nothing
        // beyond whether the server is up -- which is already observable by asking it for a page.
        "/actuator/health",
        "/actuator/health/**"
    )

    /** Public paths that use optional authentication: a supplied token is still validated. */
    val optionalAuthPatterns: List<String> = listOf(
        // The configuration page also accepts a master key, so the filter chain has to let it
        // through to the controller's own access check.
        "/admin/config",
        "/${ApiEndpoints.SEND_OTP}",
        "/users"
    )

    /**
     * Optional-auth paths that are nonetheless exempt from CSRF.
     *
     * Only [ApiEndpoints.SEND_OTP], and it belongs here for the same reason `/login` and `/signup`
     * do: it is a step in signing up, taken before anyone has a token to present. Being on
     * [optionalAuthPatterns] rather than [purelyPublicPatterns] left it out of the exemption
     * below, so every call to it from a browser app — which carries cookies, and so is not a
     * header-only bearer request either — came back 403 with no way for the client to know a CSRF
     * token was what it lacked. Native clients avoid it only by keeping no cookie jar at all.
     *
     * The exemption gives a forged request nothing. The endpoint reads the caller's identity, but
     * only to *refuse* — a signed-in user may not request a code for an address that belongs to
     * someone else — so a cross-site call can at most do what an anonymous one already can: ask for
     * a code to be sent to an address the sender named, which the abuse limits already bound. It
     * grants no authority and changes nothing about the victim's account; the code still has to be
     * presented to `/verify-otp`, which is authenticated and not exempt.
     */
    private val csrfExemptOptionalAuthPatterns: List<String> = listOf(
        "/${ApiEndpoints.SEND_OTP}"
    )

    fun purelyPublicRequestMatcher(): RequestMatcher {
        val builder = PathPatternRequestMatcher.withDefaults()
        val matchers = purelyPublicPatterns.map { builder.matcher(it) } +
                builder.matcher(HttpMethod.POST, "/$THIRD_FACTOR_KYC")
        return OrRequestMatcher(matchers)
    }

    /**
     * Paths that stay public but must still carry a CSRF token, because a forged call to them has a
     * real effect on a signed-in victim.
     */
    private val csrfProtectedPublicPatterns: Set<String> = setOf(
        // Establishes the session. Without a token an attacker can silently sign a victim into an
        // account the attacker controls, and then read back whatever the victim does in it.
        "/session-login",
        // Ends it. Forgeable logout is only a nuisance, but it is a nuisance an attacker can inflict
        // repeatedly, and nothing needs the exemption: the console sends the token, and native
        // clients sign out with an `Authorization` header, which is exempt on its own account.
        "/${ApiEndpoints.LOGOUT}"
    )

    /**
     * Endpoints exempt from CSRF protection.
     *
     * These deliberately ignore ambient credentials — the bearer token resolver returns null for
     * them — so a forged cross-site request carries no authority and there is nothing to protect.
     * [csrfExemptOptionalAuthPatterns] is the one addition to that rule, and says there why.
     *
     * `/login` stays exempt so that native and first-run clients can sign in without first fetching
     * a token, and is not the login-CSRF hole that would suggest: it reads its credentials with
     * `@RequestBody`, so it accepts JSON only. A cross-site HTML form cannot produce that content
     * type — forms may only send form-encoded, multipart or plain text — and a scripted request that
     * sets it triggers a CORS preflight, which fails for any origin not on the allow-list. This is
     * asserted in `CsrfProtectionIntegrationTest`, since it is a property of how `/login` parses its
     * body and would otherwise be silently lost if that ever changed.
     */
    fun csrfExemptRequestMatcher(): RequestMatcher {
        val builder = PathPatternRequestMatcher.withDefaults()
        val matchers = (purelyPublicPatterns + csrfExemptOptionalAuthPatterns)
            .filterNot { it in csrfProtectedPublicPatterns }
            .map { builder.matcher(it) } +
                builder.matcher(HttpMethod.POST, "/$THIRD_FACTOR_KYC")
        return OrRequestMatcher(matchers)
    }
}
