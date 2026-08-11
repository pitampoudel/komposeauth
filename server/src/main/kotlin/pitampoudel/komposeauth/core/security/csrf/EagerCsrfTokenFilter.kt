package pitampoudel.komposeauth.core.security.csrf

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter
import java.util.function.Supplier

/**
 * Settles the CSRF token before a page starts rendering, so its cookie can still be written.
 *
 * Spring Security defers the token: nothing is generated, and [CrossOriginCsrfTokenRepository]
 * is never asked to save anything, until something actually reads it. For a server-rendered page
 * that something is the template, which reads `${_csrf}` wherever the hidden field happens to sit.
 * By then the servlet container may already have flushed the first 8KB of the response — and once
 * the response is committed, `addCookie` is silently discarded. No exception, no log line, just no
 * `Set-Cookie`.
 *
 * The login page is where that bit. It carries its own stylesheet, so `_csrf` fell at roughly byte
 * 9500 of a 15KB page, past Tomcat's default 8KB buffer. The form rendered a perfectly good token
 * and the browser was given no cookie to match it against, so every password sign-in came back
 * `403` on a whitelabel error page. The admin pages were unaffected only by luck of layout: their
 * `<meta name="_csrf">` sits in `<head>`, around byte 150.
 *
 * Reading the token here — while nothing has been written yet — puts the save back inside the
 * window where it can take effect, and does it for every page rather than for whichever ones happen
 * to mention the token early enough.
 *
 * Scoped to requests that will render a page. Doing it for every API call would attach a
 * `Set-Cookie` to responses that never asked for one, and those callers authenticate with a header
 * and are exempt from CSRF anyway.
 */
class EagerCsrfTokenFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (rendersAPage(request)) {
            // A `SupplierCsrfToken` in practice — a CsrfToken that resolves on first read. The
            // Supplier branch is for the shape other request handlers set it to; either way,
            // reading the value is what triggers the save.
            when (val attribute = request.getAttribute(CsrfToken::class.java.name)) {
                is CsrfToken -> attribute.token
                is Supplier<*> -> (attribute.get() as? CsrfToken)?.token
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun rendersAPage(request: HttpServletRequest): Boolean {
        if (request.method != HttpMethod.GET.name()) return false
        return request.getHeader("Accept")?.contains("text/html", ignoreCase = true) == true
    }
}
