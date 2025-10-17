package com.vardansoft.komposeauth.core.providers

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

class OAuth2PublicClientAuthProvider(
    val registeredClientRepository: RegisteredClientRepository
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication as OAuth2PublicClientAuthToken

        if (token.clientAuthenticationMethod != ClientAuthenticationMethod.NONE) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT)
        }

        val registeredClient = registeredClientRepository.findByClientId(token.clientId)

        if (registeredClient == null) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
        }

        return OAuth2ClientAuthenticationToken(
            registeredClient,
            token.clientAuthenticationMethod,
            token.principal
        )
    }

    override fun supports(authentication: Class<*>): Boolean {
        return OAuth2PublicClientAuthToken::class.java.isAssignableFrom(authentication)
    }
}
