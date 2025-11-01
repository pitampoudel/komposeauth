package pitampoudel.komposeauth.core.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.util.StringUtils

/**
 * Resolves bearer token from either the Authorization header (standard) or
 * from an HttpOnly cookie named `accessToken` for browser-based clients.
 *
 * Order of precedence:
 * 1) Authorization: Bearer <token>
 * 2) Cookie: accessToken
 */
class CookieOrHeaderBearerTokenResolver : BearerTokenResolver {
    override fun resolve(request: HttpServletRequest): String? {
        // 1) Try Authorization header
        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ", ignoreCase = true)) {
            val token = authorization.substringAfter(" ")
            if (token.isNotBlank()) return token
        }

        // 2) Fallback to accessToken cookie
        val cookieToken = request.cookies?.firstOrNull { it.name == "accessToken" }?.value
        return cookieToken?.takeIf { it.isNotBlank() }
    }
}