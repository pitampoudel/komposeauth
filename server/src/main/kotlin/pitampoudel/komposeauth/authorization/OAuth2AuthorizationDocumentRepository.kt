package pitampoudel.komposeauth.authorization

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface OAuth2AuthorizationDocumentRepository : MongoRepository<OAuth2AuthorizationDocument, String> {
    fun findByAuthorizationCodeValue(value: String): OAuth2AuthorizationDocument?
    /** An authorization waiting for the user's consent, by the state the consent form posts back. */
    fun findByState(value: String): OAuth2AuthorizationDocument?
    fun findByAccessTokenValue(value: String): OAuth2AuthorizationDocument?
    fun findByRefreshTokenValue(value: String): OAuth2AuthorizationDocument?
    fun findByOidcIdTokenValue(value: String): OAuth2AuthorizationDocument?
    fun deleteAllByPrincipalName(principalName: String)
}