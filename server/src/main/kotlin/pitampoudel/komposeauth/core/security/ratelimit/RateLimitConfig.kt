package pitampoudel.komposeauth.core.security.ratelimit

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class RateLimitConfig {

    /**
     * Registered ahead of the security filter chains so throttled requests are turned away before
     * any password hashing or SMS provider call happens.
     */
    @Bean
    fun rateLimitFilterRegistration(
        rateLimiter: RateLimiter
    ): FilterRegistrationBean<RateLimitFilter> {
        val registration = FilterRegistrationBean(RateLimitFilter(rateLimiter))
        registration.order = Ordered.HIGHEST_PRECEDENCE + 100
        registration.addUrlPatterns("/*")
        return registration
    }
}
