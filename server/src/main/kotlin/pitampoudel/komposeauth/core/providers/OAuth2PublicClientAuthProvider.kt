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

        // Without this check a client_id alone authenticates *any* registered client, including
        // confidential ones that are supposed to prove possession of a client secret — the secret
        // becomes optional and knowing the (public) client_id is enough to mint tokens.
        if (!registeredClient.clientAuthenticationMethods.contains(ClientAuthenticationMethod.NONE)) {
            log.warn(
                "Public client auth rejected: client '{}' is not registered for the 'none' authentication method",
                token.clientId
            )
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)
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
