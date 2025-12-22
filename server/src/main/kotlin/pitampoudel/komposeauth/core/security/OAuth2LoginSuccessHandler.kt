package pitampoudel.komposeauth.core.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.user.service.UserService

/**
 * On successful Google OAuth2 login (OIDC), we:
 * 1) Map the Google user into our own User (create if needed)
 * 3) Replace the SecurityContext authentication with our own principal (userId + roles)
 *
 * This keeps the app's SecurityContext consistent regardless of login method.
 */
@Component
class OAuth2LoginSuccessHandler(
    private val userService: UserService,
    private val securityContextRepository: HttpSessionSecurityContextRepository
) : SavedRequestAwareAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth = authentication as? OAuth2AuthenticationToken
            ?: return super.onAuthenticationSuccess(request, response, authentication)

        val oidcUser = oauth.principal as? OidcUser
            ?: return super.onAuthenticationSuccess(request, response, authentication)

        val idTokenValue = oidcUser.idToken?.tokenValue
        val user = if (!idTokenValue.isNullOrBlank()) {
            userService.findOrCreateUserByGoogleIdToken(idTokenValue)
        } else return super.onAuthenticationSuccess(request, response, authentication)

        val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_$it") }

        // Put our own authentication in the SecurityContext for the remainder of the request + session.
        val appAuth = UsernamePasswordAuthenticationToken(user.id.toHexString(), null, authorities)
        SecurityContextHolder.getContext().authentication = appAuth
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response)

        // Continue with default saved-request redirect behavior.
        super.onAuthenticationSuccess(request, response, appAuth)
    }
}

