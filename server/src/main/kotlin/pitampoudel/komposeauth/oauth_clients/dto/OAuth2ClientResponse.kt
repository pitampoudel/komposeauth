package pitampoudel.komposeauth.oauth_clients.dto

import org.springframework.security.oauth2.core.AuthorizationGrantType

data class OAuth2ClientResponse(
    val clientId: String,
    val clientSecret: String,
    val clientName: String,
    val redirectUris: Set<String>,
    val grantTypes: Set<AuthorizationGrantType>,
    val scopes: Set<String>
)
