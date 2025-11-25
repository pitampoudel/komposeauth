package pitampoudel.komposeauth.core.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Collections

/**
 * A lightweight filter that makes JWT in ACCESS_TOKEN cookie available to the
 * standard Bearer-token processing by injecting an Authorization header when missing.
 *
 * This lets clients authenticate either via Authorization: Bearer <jwt> header
 * or via ACCESS_TOKEN cookie without duplicating JWT verification logic.
 */
class JwtCookieAuthFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val hasAuthHeader = !request.getHeader("Authorization").isNullOrBlank()
        if (hasAuthHeader) {
            filterChain.doFilter(request, response)
            return
        }

        val cookieToken = request.cookies?.firstOrNull { it.name == "ACCESS_TOKEN" }?.value

        if (cookieToken.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        val wrapped = object : HttpServletRequestWrapper(request) {
            override fun getHeader(name: String): String? {
                return when {
                    name.equals("Authorization", ignoreCase = true) -> "Bearer $cookieToken"
                    else -> super.getHeader(name)
                }
            }

            override fun getHeaders(name: String): java.util.Enumeration<String> {
                return if (name.equals("Authorization", ignoreCase = true)) {
                    Collections.enumeration(listOf("Bearer $cookieToken"))
                } else super.getHeaders(name)
            }

            override fun getHeaderNames(): java.util.Enumeration<String> {
                val names = super.getHeaderNames().toList().toMutableSet()
                names.add("Authorization")
                return Collections.enumeration(names)
            }
        }

        filterChain.doFilter(wrapped, response)
    }
}
