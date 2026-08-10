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
import java.time.Duration

/**
 * Per-client-IP throttling for the unauthenticated endpoints an attacker can hammer: password
 * login, OTP issuing and verification, and password-reset mail.
 *
 * Runs after Spring's `ForwardedHeaderFilter` (see `server.forward-headers-strategy`), so
 * `remoteAddr` already reflects the real client when the app sits behind a trusted proxy.
 */
class RateLimitFilter(
    private val rateLimiter: RateLimiter
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class Rule(
        val method: HttpMethod,
        val path: String,
        val limit: Int,
        val window: Duration
    )

    private val rules = listOf(
        // Password and OTP login: enough headroom for a person fumbling their password, far too
        // little for credential stuffing.
        Rule(HttpMethod.POST, "/${ApiEndpoints.LOGIN}", 10, Duration.ofMinutes(5)),
        Rule(HttpMethod.POST, "/session-login", 10, Duration.ofMinutes(5)),
        // Each of these sends an SMS or an email on someone else's behalf.
        Rule(HttpMethod.POST, "/${ApiEndpoints.SEND_OTP}", 5, Duration.ofMinutes(15)),
        Rule(HttpMethod.POST, "/${ApiEndpoints.VERIFY_OTP}", 10, Duration.ofMinutes(15)),
        Rule(HttpMethod.PUT, "/${ApiEndpoints.RESET_PASSWORD}", 5, Duration.ofHours(1)),
        Rule(HttpMethod.POST, "/${ApiEndpoints.RESET_PASSWORD}", 10, Duration.ofHours(1))
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val rule = rules.firstOrNull {
            it.method.matches(request.method) && it.path == request.requestURI
        }
        if (rule == null) {
            filterChain.doFilter(request, response)
            return
        }

        val key = "ip:${request.remoteAddr}:${rule.method}:${rule.path}"
        if (rateLimiter.tryAcquire(key, rule.limit, rule.window)) {
            filterChain.doFilter(request, response)
            return
        }

        val retryAfter = rateLimiter.retryAfterSeconds(key)
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
