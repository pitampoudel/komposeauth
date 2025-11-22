package pitampoudel.komposeauth.oauth_clients.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import java.time.Instant

@Document(collection = "oauth2_clients")
@TypeAlias("oauth2_client")
data class OAuth2Client(
    @Id
    val clientId: String,
    val clientSecret: String?,
    val clientName: String,
    val clientAuthenticationMethods: Set<ClientAuthenticationMethod>,
    val authorizationGrantTypes: Set<AuthorizationGrantType>,
    val redirectUris: Set<String>,
    val scopes: Set<String>,
    val requireAuthorizationConsent: Boolean,
    val clientUri: String?,
    val logoUri: String?,
    @CreatedDate
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    val updatedAt: Instant = Instant.now()
) {
    companion object {
        const val SCOPE_READ_ANY_USER = "user.read.any"
    }
}

