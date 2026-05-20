package pitampoudel.komposeauth.oauth_clients.dto

data class OAuth2ClientResponse(
    val clientName: String,
    val clientId: String,
    val clientSecret: String?,
    val redirectUris: Set<String>,
    val clientUri: String?,
    val logoUri: String?,
    val scopes: Set<String>
)
