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
        "/${ApiEndpoints.LOGIN}",
        "/${ApiEndpoints.LOGOUT}",
        "/signup",
        "/${ApiEndpoints.LOGIN_OPTIONS}",
        "/${ApiEndpoints.VERIFY_EMAIL}",
        "/${ApiEndpoints.RESET_PASSWORD}",
        "/reset-password",
        "/countries.json",
        "/.well-known/**",
        "/setup"
    )

    /** Public paths that use optional authentication: a supplied token is still validated. */
    val optionalAuthPatterns: List<String> = listOf(
        // Both addresses serve the same configuration page, and both accept the master key, so the
        // filter chain has to let them through to the controller's own access check.
        "/config",
        "/admin/config",
        "/${ApiEndpoints.SEND_OTP}",
        "/users"
    )

    fun purelyPublicRequestMatcher(): RequestMatcher {
        val builder = PathPatternRequestMatcher.withDefaults()
        val matchers = purelyPublicPatterns.map { builder.matcher(it) } +
                builder.matcher(HttpMethod.POST, "/$THIRD_FACTOR_KYC")
        return OrRequestMatcher(matchers)
    }

    /**
     * Endpoints exempt from CSRF protection.
     *
     * These deliberately ignore ambient credentials — the bearer token resolver returns null for
     * them — so a forged cross-site request carries no authority and there is nothing to protect.
     * `/session-login` is *not* exempt: it is what establishes the session, and without a token an
     * attacker can silently sign a victim into an account they control.
     */
    fun csrfExemptRequestMatcher(): RequestMatcher {
        val builder = PathPatternRequestMatcher.withDefaults()
        val matchers = purelyPublicPatterns
            .filterNot { it == "/session-login" }
            .map { builder.matcher(it) } +
                builder.matcher(HttpMethod.POST, "/$THIRD_FACTOR_KYC")
        return OrRequestMatcher(matchers)
    }
}
