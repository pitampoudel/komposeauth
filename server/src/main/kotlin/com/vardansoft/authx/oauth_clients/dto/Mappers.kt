package com.vardansoft.authx.oauth_clients.dto

import com.vardansoft.authx.oauth_clients.entity.OAuth2Client
import org.bson.types.ObjectId
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration
import java.time.Instant
import java.util.*

fun OAuth2Client.toRegisteredClient(): RegisteredClient {

    val builder = RegisteredClient.withId(this.clientId)
        .clientSecret(this.clientSecret)
        .clientIdIssuedAt(this.createdAt)
        .clientId(this.clientId)
        .clientName(this.clientName)
        .clientAuthenticationMethods { methods ->
            methods.addAll(this.clientAuthenticationMethods)
        }
        .authorizationGrantTypes { grants ->
            grants.addAll(this.authorizationGrantTypes)
        }
        .redirectUris { uris -> uris.addAll(this.redirectUris) }
        .scopes { scopes -> scopes.addAll(this.scopes) }
        .clientSettings(
            ClientSettings.builder()
                .requireAuthorizationConsent(this.requireAuthorizationConsent)
                .requireProofKey(true)
                .build()
        )
        .tokenSettings(
            TokenSettings.builder()
                .reuseRefreshTokens(false)
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .accessTokenTimeToLive(Duration.ofMinutes(15))
                .build()
        )

    return builder.build()
}

fun CreateClientRequest.toEntity(): OAuth2Client {
    val id = clientId?.let { ObjectId(it) } ?: ObjectId()
    val secret = UUID.randomUUID().toString().replace("-", "") +
            UUID.randomUUID().toString().replace("-", "")
    val authenticationMethods: Set<ClientAuthenticationMethod> = setOf(
        ClientAuthenticationMethod.CLIENT_SECRET_POST,
        ClientAuthenticationMethod.NONE
    )
    val grantTypes: Set<AuthorizationGrantType> = setOf(
        AuthorizationGrantType.AUTHORIZATION_CODE,
        AuthorizationGrantType.REFRESH_TOKEN,
        AuthorizationGrantType.CLIENT_CREDENTIALS,
        GOOGLE_ID_TOKEN_GRANT_TYPE
    )
    return OAuth2Client(
        clientId = id.toHexString(),
        clientSecret = secret,
        clientName = this.clientName,
        clientAuthenticationMethods = authenticationMethods,
        authorizationGrantTypes = grantTypes,
        redirectUris = this.redirectUris,
        clientUri = this.clientUri,
        logoUri = this.logoUri,
        scopes = setOf(OidcScopes.PROFILE, OidcScopes.EMAIL, OidcScopes.OPENID, "offline_access"),
        requireAuthorizationConsent = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}

fun OAuth2Client.toClientRegistrationResponse(): OAuth2ClientResponse {
    return OAuth2ClientResponse(
        clientId = clientId,
        clientSecret = clientSecret,
        clientName = clientName,
        redirectUris = redirectUris,
        grantTypes = authorizationGrantTypes,
        scopes = scopes
    )
}
