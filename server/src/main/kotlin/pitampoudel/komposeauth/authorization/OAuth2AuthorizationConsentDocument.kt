package pitampoudel.komposeauth.authorization

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/** What a user agreed to let one client have, so they are not asked again on every sign-in. */
@Document(collection = "oauth2_authorization_consents")
@TypeAlias("oauth2_authorization_consent")
data class OAuth2AuthorizationConsentDocument(
    /** [consentId], so saving a consent again replaces the one before it. */
    @Id val id: String,
    val registeredClientId: String,
    @Indexed val principalName: String,
    val authorities: Set<String>,
)

/** A principal name here is a user id, which holds no ':', so the pair can't be spelled two ways. */
fun consentId(registeredClientId: String, principalName: String) = "$principalName:$registeredClientId"
