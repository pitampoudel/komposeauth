package com.vardansoft.auth.core.providers

import com.vardansoft.auth.authorizations.service.MongoOAuth2AuthorizationService
import com.vardansoft.auth.core.providers.utils.*
import com.vardansoft.auth.user.service.UserService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator


class GoogleIdTokenGrantAuthProvider(
    private val authorizationService: MongoOAuth2AuthorizationService,
    private val tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>,
    private val userService: UserService,
    private val registeredClientRepository: RegisteredClientRepository,
    private val googleClientId: String
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val googleIdTokenAuthToken = authentication as GoogleIdTokenAuthToken

        val registeredClient = registeredClientRepository.findByClientId(
            googleIdTokenAuthToken.clientId
        ) ?: throw OAuth2AuthenticationException(
            OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client not found", null)
        )

        if (!registeredClient.authorizationGrantTypes.contains(googleIdTokenAuthToken.grantType)) {
            throw OAuth2AuthenticationException(
                OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, "Grant type not supported", null)
            )
        }
        val payload = validateGoogleIdToken(
            clientId = googleClientId,
            googleIdTokenAuthToken.idToken
        )
        val user = userService.findOrCreateUserByEmail(
            email = payload["email"] as String,
            firstName = payload["given_name"] as String,
            lastName = payload["family_name"] as String,
            picture = payload["picture"] as? String
        )
        if (payload["email_verified"] == true) {
            userService.emailVerified(user.id)
        }

        val ctx = authorizationContext()
        val accessToken = tokenGenerator.generateAccessToken(
            authorizationServerContext = ctx,
            registeredClient = registeredClient,
            user = user,
            googleIdToken = googleIdTokenAuthToken,
        )

        val refreshToken = tokenGenerator.generateRefreshToken(
            authorizationServerContext = ctx,
            registeredClient = registeredClient,
            user = user,
            googleIdToken = googleIdTokenAuthToken,
        )
        val authorization = createAuthorization(
            registeredClient = registeredClient,
            accessToken = accessToken,
            refreshToken = refreshToken,
            googleIdToken = googleIdTokenAuthToken,
            user = user
        )
        authorizationService.save(authorization)
        return OAuth2AccessTokenAuthenticationToken(
            registeredClient,
            googleIdTokenAuthToken,
            accessToken,
            refreshToken
        )
    }

    override fun supports(authentication: Class<*>): Boolean {
        return GoogleIdTokenAuthToken::class.java.isAssignableFrom(authentication)
    }
}
