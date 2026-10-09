package pitampoudel.komposeauth.authorization

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface OAuth2AuthorizationConsentDocumentRepository : MongoRepository<OAuth2AuthorizationConsentDocument, String> {
    fun deleteAllByPrincipalName(principalName: String)
}
