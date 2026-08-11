package pitampoudel.komposeauth.core.security.ratelimit

import jakarta.servlet.DispatcherType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.ForwardedHeaderFilter
import java.time.Clock

@Configuration
class RateLimitConfig {

    /**
     * Window boundaries are derived from this, so tests can advance time instead of sleeping.
     * UTC keeps the boundaries identical on every instance regardless of host timezone.
     */
    @Bean
    fun rateLimitClock(): Clock = Clock.systemUTC()

    /**
     * Registered ahead of everything that touches the request, including [ForwardedHeaderFilter]
     * below.
     *
     * Two reasons, and the second is the load-bearing one. Throttled requests are turned away before
     * any password hashing or SMS provider call happens; and the limiter gets to see the request as
     * it arrived — real peer address, `X-Forwarded-For` intact — rather than after
     * `ForwardedHeaderFilter` has overwritten `remoteAddr` with the header's leftmost entry and
     * stripped the header away. That entry is chosen by whoever sent the request, so a limiter
     * reading it counts a different "client" on every attempt. See [ClientIpResolver].
     *
     * One notch in from the very front, which is claimed by the unhandled-error reporter — that one
     * reads nothing and decides nothing, it only wraps the chain so a failure in here is reported
     * rather than swallowed.
     */
    @Bean
    fun rateLimitFilterRegistration(
        rateLimiter: RateLimiter,
        properties: RateLimitProperties,
        clientIpResolver: ClientIpResolver
    ): FilterRegistrationBean<RateLimitFilter> {
        val registration = FilterRegistrationBean(
            RateLimitFilter(rateLimiter, properties, clientIpResolver)
        )
        registration.order = Ordered.HIGHEST_PRECEDENCE + 10
        registration.addUrlPatterns("/*")
        return registration
    }

    /**
     * The same filter Spring Boot would register, moved one notch later so the rate limiter runs
     * first.
     *
     * Boot registers this at `HIGHEST_PRECEDENCE` and there is no order below that to claim, so the
     * only way to get in front of it is to register it here instead — which Boot allows for, backing
     * off through `@ConditionalOnMissingFilterBean`. The property condition is copied from Boot so
     * the filter still appears only when forwarded headers are actually being trusted.
     */
    @Bean
    @ConditionalOnProperty(name = ["server.forward-headers-strategy"], havingValue = "framework")
    fun forwardedHeaderFilter(): FilterRegistrationBean<ForwardedHeaderFilter> {
        val registration = FilterRegistrationBean(ForwardedHeaderFilter())
        registration.setDispatcherTypes(
            DispatcherType.REQUEST,
            DispatcherType.ASYNC,
            DispatcherType.ERROR
        )
        registration.order = Ordered.HIGHEST_PRECEDENCE + 50
        return registration
    }
}
