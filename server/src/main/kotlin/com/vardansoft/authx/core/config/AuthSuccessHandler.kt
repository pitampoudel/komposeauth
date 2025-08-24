package com.vardansoft.authx.core.config

import com.vardansoft.authx.credentials.dto.UpdateCredentialRequest
import com.vardansoft.authx.credentials.entity.Credential
import com.vardansoft.authx.credentials.service.CredentialService
import com.vardansoft.authx.user.entity.User
import com.vardansoft.authx.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class AuthSuccessHandler(
    val userService: UserService,
    val authorizedClientService: OAuth2AuthorizedClientService,
    val credentialService: CredentialService
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val user: User
        when (val principal = authentication.principal) {
            is OAuth2User -> {
                val email: String = principal.getAttribute<String>("email").orEmpty()
                val firstName: String = principal.getAttribute<String>("given_name").orEmpty()
                val lastName: String = principal.getAttribute<String>("family_name").orEmpty()
                val picture: String = principal.getAttribute<String>("picture").orEmpty()
                val emailVerified: Boolean = principal.getAttribute<Boolean>("emailVerified") == true
                user = userService.findOrCreateUserByEmail(email, firstName, lastName, picture)
                if (emailVerified) userService.emailVerified(user.id)

                (authentication as OAuth2AuthenticationToken).let { oAuth2Token ->
                    authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>(
                        oAuth2Token.authorizedClientRegistrationId,
                        oAuth2Token.name
                    ).let { oAuth2AuthorizedClient ->
                        credentialService.updateCredential(
                            userId = user.id,
                            req = UpdateCredentialRequest(
                                provider = Credential.Provider.GOOGLE,
                                accessToken = oAuth2AuthorizedClient.accessToken.tokenValue,
                                refreshToken = oAuth2AuthorizedClient.refreshToken?.tokenValue
                            )
                        )
                    }
                }
            }

            is UserDetails -> {
                user = userService.findUserByEmail(principal.username)
                    ?: throw IllegalStateException("User not found with email: ${principal.username}")
            }

            else -> {
                throw IllegalStateException(
                    "Authenticated not supported for: ${authentication.principal.javaClass.name}"
                )
            }
        }

        val auth = user.asAuthToken()
        SecurityContextHolder.getContext().authentication = auth

        SavedRequestAwareAuthenticationSuccessHandler().onAuthenticationSuccess(
            request,
            response,
            auth
        )
    }
}