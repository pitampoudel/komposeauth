package pitampoudel.komposeauth.core.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class DualPathServletConfig {
    @Bean
    fun authPrefixForwardFilter(): FilterRegistrationBean<OncePerRequestFilter> {
        return FilterRegistrationBean<OncePerRequestFilter>(object : OncePerRequestFilter() {
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain
            ) {
                val contextPath = request.contextPath ?: ""
                val pathWithinApp = request.requestURI.substring(contextPath.length)
                val needsForward = pathWithinApp.startsWith("/auth")
                if (needsForward) {
                    val target = pathWithinApp.removePrefix("/auth")
                    request.getRequestDispatcher(target).forward(request, response)
                    return
                }
                filterChain.doFilter(request, response)
            }

        }).apply {
            addUrlPatterns("/auth", "/auth/*")
            order = Ordered.HIGHEST_PRECEDENCE
        }
    }
}
