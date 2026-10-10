package pitampoudel.komposeauth.core.security.ratelimit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter
import pitampoudel.komposeauth.core.domain.ApiEndpoints

/**
 * Per-client-IP throttling for the unauthenticated endpoints an attacker can hammer: password
 * login, OTP issuing and verification, and password-reset mail.
 *
 * Runs before `ForwardedHeaderFilter` and works the client address out itself via [ClientIpResolver],
 * because `remoteAddr` under `server.forward-headers-strategy: framework` is taken from the leftmost
 * `X-Forwarded-For` entry — a value the caller supplies, and can vary per request to be counted as a
 * new client every time.
 */
class RateLimitFilter(
    private val rateLimiter: RateLimiter,
    private val properties: RateLimitProperties,
    private val clientIpResolver: ClientIpResolver
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class Rule(
        val method: HttpMethod,
        val path: String,
        val quota: RateLimitProperties.Rule
    ) {
        // The path as the dispatcher routes it, decoded and without path parameters, so that
        // `/%6Cogin` or `/login;x` is counted as `/login` rather than slipping past every rule.
        val matcher: RequestMatcher = PathPatternRequestMatcher.withDefaults().matcher(method, path)
    }

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

        val rule = rules.firstOrNull { it.matcher.matches(request) }
        if (rule == null) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = clientIpResolver.resolve(request)
        val key = "ip:$clientIp:${rule.method}:${rule.path}"
        val decision = rateLimiter.check(key, rule.quota.limit, rule.quota.window)
        if (decision.allowed) {
            filterChain.doFilter(request, response)
            return
        }

        val retryAfter = decision.retryAfterSeconds
        log.warn(
            "Rate limit exceeded for {} {} from {}",
            request.method,
            rule.path,
            clientIp
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
