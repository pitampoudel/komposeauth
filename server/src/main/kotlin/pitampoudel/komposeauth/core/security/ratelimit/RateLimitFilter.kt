package pitampoudel.komposeauth.core.security.ratelimit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import pitampoudel.komposeauth.core.domain.ApiEndpoints

/**
 * Per-client-IP throttling for the unauthenticated endpoints an attacker can hammer: password
 * login, OTP issuing and verification, and password-reset mail.
 *
 * Runs after Spring's `ForwardedHeaderFilter` (see `server.forward-headers-strategy`), so
 * `remoteAddr` already reflects the real client when the app sits behind a trusted proxy.
 */
class RateLimitFilter(
    private val rateLimiter: RateLimiter,
    private val properties: RateLimitProperties
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class Rule(
        val method: HttpMethod,
        val path: String,
        val quota: RateLimitProperties.Rule
    )

    private val rules: List<Rule> = listOf(
        Rule(HttpMethod.POST, "/${ApiEndpoints.LOGIN}", properties.login),
        Rule(HttpMethod.POST, "/session-login", properties.login),
        Rule(HttpMethod.POST, "/${ApiEndpoints.SEND_OTP}", properties.otpSend),
        Rule(HttpMethod.POST, "/${ApiEndpoints.VERIFY_OTP}", properties.otpVerify),
        Rule(HttpMethod.PUT, "/${ApiEndpoints.RESET_PASSWORD}", properties.passwordResetRequest),
        Rule(HttpMethod.POST, "/${ApiEndpoints.RESET_PASSWORD}", properties.passwordResetSubmit)
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!properties.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        val rule = rules.firstOrNull {
            it.method.matches(request.method) && it.path == request.requestURI
        }
        if (rule == null) {
            filterChain.doFilter(request, response)
            return
        }

        val key = "ip:${request.remoteAddr}:${rule.method}:${rule.path}"
        val decision = rateLimiter.check(key, rule.quota.limit, rule.quota.window)
        if (decision.allowed) {
            filterChain.doFilter(request, response)
            return
        }

        val retryAfter = decision.retryAfterSeconds
        log.warn(
            "Rate limit exceeded for {} {} from {}",
            request.method,
            request.requestURI,
            request.remoteAddr
        )
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader("Retry-After", retryAfter.toString())
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.writer.write(
            """{"type":"about:blank","title":"Too Many Requests","status":429,""" +
                    """"detail":"Too many requests. Try again in $retryAfter seconds."}"""
        )
    }
}
