package pitampoudel.komposeauth.oauth_clients.dto

import kotlinx.serialization.Serializable
import org.springframework.security.oauth2.core.oidc.OidcScopes


@Serializable
data class CreateClientRequest(
    val clientName: String,
    val clientId: String? = null,
    val clientSecret: String? = null,
    /** A browser or mobile app, which can't keep a secret: it signs in with PKCE alone and holds none. */
    val publicClient: Boolean = false,
    val redirectUris: Set<String> = emptySet(),
    val clientUri: String? = null,
    val logoUri: String? = null,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlDays: Long,
    val scopes: Set<String> = setOf(
        OidcScopes.PROFILE,
        OidcScopes.EMAIL,
        OidcScopes.OPENID,
        "offline_access"
    ),
)
