package pitampoudel.komposeauth.core.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Anything thrown from here reaches the servlet container, not the exception handlers.
     *
     * This runs inside the sign-in filter, so `@ControllerAdvice` never sees it: the visitor gets a
     * whitelabel 500 on the OAuth callback URL, naming no cause, and the operator gets a stack trace
     * only if they happen to be reading logs at the time. Neither says "sign-in failed". Turn it
     * into the same outcome as any other failed sign-in — back to the page, with the reason logged
     * against the provider it came from.
     */
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        try {
            establishSession(request, response, authentication)
        } catch (ex: Exception) {
            val provider = (authentication as? OAuth2AuthenticationToken)?.authorizedClientRegistrationId
            log.error("Could not complete sign-in through '{}'", provider ?: "oauth2", ex)
            response.sendRedirect("/session-login?error=provider")
        }
    }

    private fun establishSession(
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

