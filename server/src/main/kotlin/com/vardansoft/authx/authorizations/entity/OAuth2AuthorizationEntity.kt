package com.vardansoft.authx.authorizations.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "oauth2_authorizations")
@TypeAlias("oauth2_authorization")
data class OAuth2AuthorizationEntity(
    @Id
    val id: String,

    @Indexed
    val registeredClientId: String,

    @Indexed
    val principalName: String,

    val authorizationGrantType: String,

    val authorizedScopes: Set<String>?,

    val attributes: String,

    val state: String?,

    // Authorization Code
    val authorizationCodeValue: String?,
    val authorizationCodeIssuedAt: Instant?,
    val authorizationCodeExpiresAt: Instant?,
    val authorizationCodeMetadata: String?,

    // Access Token
    val accessTokenValue: String?,
    val accessTokenIssuedAt: Instant?,
    val accessTokenExpiresAt: Instant?,
    val accessTokenMetadata: String?,
    val accessTokenType: String?,
    val accessTokenScopes: Set<String>?,

    // OIDC ID Token
    val oidcIdTokenValue: String?,
    val oidcIdTokenIssuedAt: Instant?,
    val oidcIdTokenExpiresAt: Instant?,
    val oidcIdTokenMetadata: String?,
    val odicIdTokenClaims: String?,

    // Refresh Token
    @Indexed
    val refreshTokenValue: String?,
    val refreshTokenIssuedAt: Instant?,
    val refreshTokenExpiresAt: Instant?,
    val refreshTokenMetadata: String?,

    // User Code
    val userCodeValue: String?,
    val userCodeIssuedAt: Instant?,
    val userCodeExpiresAt: Instant?,
    val userCodeMetadata: String?,

    // Device Code
    val deviceCodeValue: String?,
    val deviceCodeIssuedAt: Instant?,
    val deviceCodeExpiresAt: Instant?,
    val deviceCodeMetadata: String?
)
