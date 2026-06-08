package pitampoudel.komposeauth.core.providers

import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(javaClass)

    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication as OAuth2PublicClientAuthToken

        if (token.clientAuthenticationMethod != ClientAuthenticationMethod.NONE) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT)
        }

        val registeredClient = registeredClientRepository.findByClientId(token.clientId)

        if (registeredClient == null) {
            log.warn("Public client auth failed: unknown client_id '{}'", token.clientId)
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
        }

        log.debug("Public client authenticated: '{}'", token.clientId)
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
