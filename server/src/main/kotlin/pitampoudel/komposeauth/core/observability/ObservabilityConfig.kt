package pitampoudel.komposeauth.core.observability

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class ObservabilityConfig {

    /**
     * First in, so everything below it — the rate limiter, the forwarded-header filter, the whole
     * Spring Security chain — unwinds through [UnhandledErrorReportingFilter] on its way out.
     *
     * Ordering it any later would leave exactly the blind spots worth closing: a security filter
     * throwing, or the limiter's own datastore being unreachable.
     */
    @Bean
    fun unhandledErrorReportingFilterRegistration(): FilterRegistrationBean<UnhandledErrorReportingFilter> {
        val registration = FilterRegistrationBean(UnhandledErrorReportingFilter())
        registration.order = Ordered.HIGHEST_PRECEDENCE
        registration.addUrlPatterns("/*")
        return registration
    }
}
