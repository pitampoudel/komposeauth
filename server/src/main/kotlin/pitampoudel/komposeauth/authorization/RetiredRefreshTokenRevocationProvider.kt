package pitampoudel.komposeauth.authorization

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationToken

/**
 * Revokes a refresh token that a refresh replaced moments ago, by ending its authorization.
 *
 * A server-rendered client signing out reads its refresh token from the cookie the request arrived
 * with, which an earlier step of the same request may just have refreshed. Spring's own revocation
 * finds nothing for that token and answers 200, leaving the one that replaced it signed in. Anything
 * else falls through to Spring's.
 */
class RetiredRefreshTokenRevocationProvider(
    private val authorizationService: MongoOAuth2AuthorizationService
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication? {
        val revocation = authentication as OAuth2TokenRevocationAuthenticationToken
        // An unauthenticated client is refused by Spring's provider.
        val client = (revocation.principal as? OAuth2ClientAuthenticationToken)?.takeIf { it.isAuthenticated }
            ?: return null
        val registeredClient = client.registeredClient ?: return null
        val authorization = authorizationService.findByRetiredRefreshToken(revocation.token)
            // Another client's token is, to this one, a token it never issued: Spring's answers 200.
            ?.takeIf { it.registeredClientId == registeredClient.id }
            ?: return null
        authorizationService.remove(authorization)
        return OAuth2TokenRevocationAuthenticationToken(OAuth2RefreshToken(revocation.token, null), client)
    }

    override fun supports(authentication: Class<*>): Boolean =
        OAuth2TokenRevocationAuthenticationToken::class.java.isAssignableFrom(authentication)
}
