package pitampoudel.komposeauth.authorization

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "oauth2_authorizations")
@TypeAlias("oauth2_authorization")
data class OAuth2AuthorizationDocument(
    @Id val id: String,
    val registeredClientId: String,
    val principalName: String,
    val authorizationGrantType: String,
    val authorizedScopes: Set<String> = emptySet(),
    val state: String? = null,
    val attributes: String? = null,

    @Indexed(sparse = true) val authorizationCodeValue: String? = null,
    val authorizationCodeIssuedAt: Instant? = null,
    val authorizationCodeExpiresAt: Instant? = null,
    val authorizationCodeMetadata: String? = null,

    @Indexed(sparse = true) val accessTokenValue: String? = null,
    val accessTokenIssuedAt: Instant? = null,
    val accessTokenExpiresAt: Instant? = null,
    val accessTokenMetadata: String? = null,
    val accessTokenType: String? = null,
    val accessTokenScopes: Set<String>? = null,

    @Indexed(sparse = true) val refreshTokenValue: String? = null,
    val refreshTokenIssuedAt: Instant? = null,
    val refreshTokenExpiresAt: Instant? = null,
    val refreshTokenMetadata: String? = null,

    val oidcIdTokenValue: String? = null,
    val oidcIdTokenIssuedAt: Instant? = null,
    val oidcIdTokenExpiresAt: Instant? = null,
    val oidcIdTokenMetadata: String? = null,
    val oidcIdTokenClaims: String? = null,
)