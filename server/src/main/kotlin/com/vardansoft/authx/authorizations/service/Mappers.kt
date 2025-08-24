package com.vardansoft.authx.authorizations.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.vardansoft.authx.authorizations.entity.OAuth2AuthorizationEntity
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module

private val objectMapper = ObjectMapper().apply {
    registerModules(
        SecurityJackson2Modules.getModules(
            MongoOAuth2AuthorizationService::class.java.getClassLoader()
        )
    )
    registerModule(OAuth2AuthorizationServerJackson2Module())
}
val mapTypeRef = object : TypeReference<Map<String, Object>?>() {}
fun OAuth2Authorization.toEntity(): OAuth2AuthorizationEntity {
    val authorizationCode = this.getToken(OAuth2AuthorizationCode::class.java)
    val accessToken = this.getToken(OAuth2AccessToken::class.java)
    val refreshToken = this.getToken(OAuth2RefreshToken::class.java)
    val oidcIdToken = this.getToken(OidcIdToken::class.java)

    return OAuth2AuthorizationEntity(
        id = this.id,
        registeredClientId = this.registeredClientId,
        principalName = this.principalName,
        authorizationGrantType = this.authorizationGrantType.value,
        authorizedScopes = this.authorizedScopes,
        attributes = objectMapper.writeValueAsString(this.attributes),
        state = this.getAttribute(OAuth2ParameterNames.STATE),

        // Authorization Code
        authorizationCodeValue = authorizationCode?.token?.tokenValue,
        authorizationCodeIssuedAt = authorizationCode?.token?.issuedAt,
        authorizationCodeExpiresAt = authorizationCode?.token?.expiresAt,
        authorizationCodeMetadata = objectMapper.writeValueAsString(authorizationCode?.metadata),

        // Access Token
        accessTokenValue = accessToken?.token?.tokenValue,
        accessTokenIssuedAt = accessToken?.token?.issuedAt,
        accessTokenExpiresAt = accessToken?.token?.expiresAt,
        accessTokenMetadata = objectMapper.writeValueAsString(accessToken?.metadata),
        accessTokenType = accessToken?.token?.tokenType?.value,
        accessTokenScopes = accessToken?.token?.scopes,

        // OIDC ID Token
        oidcIdTokenValue = oidcIdToken?.token?.tokenValue,
        oidcIdTokenIssuedAt = oidcIdToken?.token?.issuedAt,
        oidcIdTokenExpiresAt = oidcIdToken?.token?.expiresAt,
        oidcIdTokenMetadata = objectMapper.writeValueAsString(oidcIdToken?.metadata),
        odicIdTokenClaims = objectMapper.writeValueAsString(oidcIdToken?.token?.claims),

        // Refresh Token
        refreshTokenValue = refreshToken?.token?.tokenValue,
        refreshTokenIssuedAt = refreshToken?.token?.issuedAt,
        refreshTokenExpiresAt = refreshToken?.token?.expiresAt,
        refreshTokenMetadata = objectMapper.writeValueAsString(refreshToken?.metadata),

        // User Code
        userCodeValue = null,
        userCodeIssuedAt = null,
        userCodeExpiresAt = null,
        userCodeMetadata = null,

        // Device Code
        deviceCodeValue = null,
        deviceCodeIssuedAt = null,
        deviceCodeExpiresAt = null,
        deviceCodeMetadata = null
    )
}

fun OAuth2AuthorizationEntity.toObject(registeredClient: RegisteredClient): OAuth2Authorization {
    val builder = OAuth2Authorization.withRegisteredClient(registeredClient)
        .id(this.id)
        .principalName(this.principalName)
        .authorizationGrantType(resolveAuthorizationGrantType(this.authorizationGrantType))
        .authorizedScopes(this.authorizedScopes ?: emptySet())
        .attributes {
            it.putAll(
                objectMapper.readValue(
                    this.attributes,
                    mapTypeRef
                ).orEmpty()
            )
        }

    // Add state
    this.state?.let {
        builder.attribute(OAuth2ParameterNames.STATE, it)
    }

    // Add Authorization Code
    if (this.authorizationCodeValue != null) {
        builder.token(
            OAuth2AuthorizationCode(
                this.authorizationCodeValue,
                this.authorizationCodeIssuedAt,
                this.authorizationCodeExpiresAt
            )
        ) { metadata ->
            metadata.putAll(
                objectMapper.readValue(
                    this.authorizationCodeMetadata,
                    mapTypeRef
                ).orEmpty()
            )
        }
    }


    // Add Access Token
    if (this.accessTokenValue != null) {
        builder.token(
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType(this.accessTokenType),
                this.accessTokenValue,
                this.accessTokenIssuedAt,
                this.accessTokenExpiresAt,
                this.accessTokenScopes
            )
        ) { metadata ->
            metadata.putAll(
                objectMapper.readValue(
                    this.accessTokenMetadata,
                    mapTypeRef
                ).orEmpty()
            )
        }
    }


    // Add Refresh Token
    if (this.refreshTokenValue != null) {
        builder.token(
            OAuth2RefreshToken(
                this.refreshTokenValue,
                this.refreshTokenIssuedAt,
                this.refreshTokenExpiresAt
            )
        ) { metadata ->
            metadata.putAll(
                objectMapper.readValue(
                    this.refreshTokenMetadata,
                    mapTypeRef
                ).orEmpty()
            )
        }
    }

    // Add OIDC ID Token
    if (this.oidcIdTokenValue != null) {
        builder.token(
            OidcIdToken(
                this.oidcIdTokenValue,
                this.oidcIdTokenIssuedAt,
                this.oidcIdTokenExpiresAt,
                objectMapper.readValue(
                    this.odicIdTokenClaims,
                    mapTypeRef
                )
            )
        ) { metadata ->
            metadata.putAll(
                objectMapper.readValue(
                    this.oidcIdTokenMetadata,
                    mapTypeRef
                ).orEmpty()
            )
        }
    }


    // Add User Code
    if (this.userCodeValue != null) {
        builder.token(
            OAuth2UserCode(
                this.userCodeValue,
                this.userCodeIssuedAt,
                this.userCodeExpiresAt
            )
        ) { metadata ->
            metadata.putAll(
                objectMapper.readValue(
                    this.userCodeMetadata,
                    mapTypeRef
                ).orEmpty()
            )
        }
    }

    // Add Device Code
    if (this.deviceCodeValue != null) {
        builder.token(
            OAuth2DeviceCode(
                this.deviceCodeValue,
                this.deviceCodeIssuedAt,
                this.deviceCodeExpiresAt
            )
        ) { metadata ->
            metadata.putAll(
                objectMapper.readValue(
                    this.deviceCodeMetadata,
                    mapTypeRef
                ).orEmpty()
            )
        }
    }
    return builder.build()
}


private fun resolveAuthorizationGrantType(authorizationGrantType: String?): AuthorizationGrantType {
    if (AuthorizationGrantType.AUTHORIZATION_CODE.value == authorizationGrantType) {
        return AuthorizationGrantType.AUTHORIZATION_CODE
    } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.value == authorizationGrantType) {
        return AuthorizationGrantType.CLIENT_CREDENTIALS
    } else if (AuthorizationGrantType.REFRESH_TOKEN.value == authorizationGrantType) {
        return AuthorizationGrantType.REFRESH_TOKEN
    } else if (AuthorizationGrantType.DEVICE_CODE.value == authorizationGrantType) {
        return AuthorizationGrantType.DEVICE_CODE
    }
    return AuthorizationGrantType(authorizationGrantType)
}
