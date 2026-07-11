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
        "/api/auth/**",
        "/users",
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
        "/config",
        "/${ApiEndpoints.SEND_OTP}"
    )

    fun purelyPublicRequestMatcher(): RequestMatcher {
        val builder = PathPatternRequestMatcher.withDefaults()
        val matchers = purelyPublicPatterns.map { builder.matcher(it) } +
            builder.matcher(HttpMethod.POST, "/$THIRD_FACTOR_KYC")
        return OrRequestMatcher(matchers)
    }
}
