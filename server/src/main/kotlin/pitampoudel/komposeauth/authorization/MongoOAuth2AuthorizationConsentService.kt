package pitampoudel.komposeauth.authorization

import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.stereotype.Service

/** Kept in Mongo, like the authorizations, so a consent outlives a restart and holds across instances. */
@Service
class MongoOAuth2AuthorizationConsentService(
    private val repository: OAuth2AuthorizationConsentDocumentRepository,
) : OAuth2AuthorizationConsentService {

    override fun save(authorizationConsent: OAuth2AuthorizationConsent) {
        repository.save(authorizationConsent.toDocument())
    }

    override fun remove(authorizationConsent: OAuth2AuthorizationConsent) {
        repository.deleteById(consentId(authorizationConsent.registeredClientId, authorizationConsent.principalName))
    }

    override fun findById(registeredClientId: String, principalName: String): OAuth2AuthorizationConsent? =
        repository.findById(consentId(registeredClientId, principalName)).orElse(null)?.toConsent()
}
