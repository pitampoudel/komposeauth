package pitampoudel.komposeauth.core.observability

import io.sentry.Sentry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Reports the failures that were reaching nobody: the ones thrown by a servlet filter.
 *
 * The only place this application tells Sentry about an exception is `GlobalExceptionHandler`, and a
 * `@ControllerAdvice` lives inside `DispatcherServlet`. Anything thrown before the request gets that
 * far unwinds straight past it — the container serves its own error page and the incident is
 * recorded nowhere. The entire Spring Security chain is filters, so the requests with the least
 * visibility are precisely the ones in sign-in: a visitor gets a whitelabel 500 on an OAuth callback
 * URL naming no cause, and no alert is raised.
 *
 * Registered outermost, so it wraps every other filter including the security chain, and rethrows
 * what it captures — the container still decides the response. This only ensures the exception is
 * reported on its way past.
 *
 * `Throwable`, not `Exception`: a missing or mismatched class on the sign-in path arrives as
 * `NoClassDefFoundError`, and that is exactly the kind of deployment fault worth being told about.
 */
class UnhandledErrorReportingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (ex: Throwable) {
            report(request, ex)
            throw ex
        }
    }

    private fun report(request: HttpServletRequest, ex: Throwable) {
        // Method and path only. The query string on these routes carries OAuth authorization codes,
        // state and ID tokens, and a crash report is not a place to put them.
        val route = "${request.method} ${request.requestURI}"
        log.error("Unhandled exception escaped the filter chain handling {}", route, ex)
        Sentry.captureException(ex) { scope ->
            scope.setTag("route", route)
            // Distinguishes these from anything GlobalExceptionHandler reports, since the two say
            // different things about where to look: this one never reached a controller.
            scope.setTag("origin", "servlet-filter")
        }
    }
}
