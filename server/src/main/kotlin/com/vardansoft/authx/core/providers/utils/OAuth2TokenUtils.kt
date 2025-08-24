package com.vardansoft.authx.core.providers.utils

import com.vardansoft.authx.core.providers.GoogleIdTokenAuthToken
import com.vardansoft.authx.user.entity.User
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.time.Instant


fun authorizationContext() = AuthorizationServerContextHolder.getContext()
    ?: throw OAuth2AuthenticationException(
        OAuth2Error(
            OAuth2ErrorCodes.SERVER_ERROR,
            "Authorization server context not available",
            null
        )
    )


fun OAuth2TokenGenerator<out OAuth2Token>.generateAccessToken(
    authorizationServerContext: AuthorizationServerContext,
    registeredClient: RegisteredClient,
    user: User,
    googleIdToken: GoogleIdTokenAuthToken,
): OAuth2AccessToken {

    val authorizedScopes = defaultScopes(registeredClient)

    val generatedToken = generate(
        DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(user.asAuthToken())
            .authorizationServerContext(authorizationServerContext)
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .authorizationGrantType(googleIdToken.grantType)
            .authorizedScopes(authorizedScopes)
            .authorizationGrant(googleIdToken)
            .build()
    ) ?: throw OAuth2AuthenticationException(
        OAuth2Error(
            OAuth2ErrorCodes.SERVER_ERROR,
            "Token generator returned null for access token",
            null
        )
    )

    // Validate generated token
    validateGeneratedToken(generatedToken, "access token")

    return OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER,
        generatedToken.tokenValue,
        generatedToken.issuedAt,
        generatedToken.expiresAt,
        authorizedScopes
    )

}


fun OAuth2TokenGenerator<out OAuth2Token>.generateRefreshToken(
    authorizationServerContext: AuthorizationServerContext,
    registeredClient: RegisteredClient,
    user: User,
    googleIdToken: GoogleIdTokenAuthToken,
): OAuth2RefreshToken {

    val generatedToken = generate(
        DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(user.asAuthToken())
            .authorizationServerContext(authorizationServerContext)
            .tokenType(OAuth2TokenType.REFRESH_TOKEN)
            .authorizationGrantType(googleIdToken.grantType)
            .authorizationGrant(googleIdToken)
            .build()
    ) ?: throw OAuth2AuthenticationException(
        OAuth2Error(
            OAuth2ErrorCodes.SERVER_ERROR,
            "Token generator returned null for refresh token",
            null
        )
    )

    // Validate generated token
    validateGeneratedToken(generatedToken, "refresh token")

    return OAuth2RefreshToken(
        generatedToken.tokenValue,
        generatedToken.issuedAt,
        generatedToken.expiresAt
    )
}

fun createAuthorization(
    registeredClient: RegisteredClient,
    accessToken: OAuth2AccessToken,
    refreshToken: OAuth2RefreshToken,
    googleIdToken: GoogleIdTokenAuthToken,
    user: User
): OAuth2Authorization {

    val authorizedScopes = defaultScopes(registeredClient)
    val userId = user.id.toString()

    // Create OIDC ID token claims
    val idTokenClaims = mapOf(
        "sub" to userId,
        "aud" to registeredClient.clientId,
        "iat" to accessToken.expiresAt,
        "exp" to accessToken.expiresAt
    )

    val oidcIdToken = OidcIdToken(
        accessToken.tokenValue, // Using access token value as ID token (consider using separate ID token)
        accessToken.issuedAt,
        accessToken.expiresAt,
        idTokenClaims
    )

    return OAuth2Authorization.withRegisteredClient(registeredClient)
        .principalName(userId)
        .authorizationGrantType(googleIdToken.grantType)
        .authorizedScopes(authorizedScopes)
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .token(oidcIdToken) { metadata ->
            metadata[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = oidcIdToken.claims
            metadata[OAuth2Authorization.Token.INVALIDATED_METADATA_NAME] = false
        }
        .attribute("java.security.Principal", user.asAuthToken())
        .build()

}


/**
 * Validates a generated token
 */
private fun validateGeneratedToken(token: OAuth2Token, tokenType: String) {
    if (token.tokenValue.isBlank()) {
        throw OAuth2AuthenticationException(
            OAuth2Error(
                OAuth2ErrorCodes.SERVER_ERROR,
                "Generated $tokenType has blank token value",
                null
            )
        )
    }

    if (token.issuedAt == null) {
        throw OAuth2AuthenticationException(
            OAuth2Error(
                OAuth2ErrorCodes.SERVER_ERROR,
                "Generated $tokenType has null issued at timestamp",
                null
            )
        )
    }

    // Validate token hasn't expired immediately
    val now = Instant.now()
    if (token.expiresAt != null && token.expiresAt!!.isBefore(now)) {
        throw OAuth2AuthenticationException(
            OAuth2Error(
                OAuth2ErrorCodes.SERVER_ERROR,
                "Generated $tokenType is already expired",
                null
            )
        )
    }
}

/**
 * Determines authorized scopes based on client configuration
 */
private fun defaultScopes(
    registeredClient: RegisteredClient
): Set<String> {
    val scopes = setOf(
        OidcScopes.OPENID,
        OidcScopes.EMAIL,
        OidcScopes.PROFILE,
        "offline_access"
    )
    val clientScopes = registeredClient.scopes
    return if (clientScopes.isNotEmpty()) {
        // Use intersection of client scopes and default scopes
        clientScopes.intersect(scopes).takeIf { it.isNotEmpty() } ?: scopes
    } else {
        scopes
    }
}
