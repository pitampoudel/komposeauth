package pitampoudel.komposeauth.authorization

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Service

@Service
class MongoOAuth2AuthorizationService(
    private val repository: OAuth2AuthorizationDocumentRepository,
    private val registeredClientRepository: RegisteredClientRepository,
    val objectMapper: ObjectMapper,
) : OAuth2AuthorizationService {


    override fun save(authorization: OAuth2Authorization) {
        repository.save(toOAuth2AuthorizationDocument(authorization, objectMapper))
    }

    override fun remove(authorization: OAuth2Authorization) {
        repository.deleteById(authorization.id)
    }


    override fun findById(id: String): OAuth2Authorization? =
        repository.findById(id).orElse(null)?.let {
            fromOAuth2AuthorizationDocument(
                doc = it,
                registeredClient = registeredClientRepository.findById(it.registeredClientId) ?: return null,
                objectMapper = objectMapper
            )
        }

    override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
        val doc = when {
            tokenType == null -> repository.findByAuthorizationCodeValue(token)
                ?: repository.findByAccessTokenValue(token)
                ?: repository.findByRefreshTokenValue(token)
                ?: repository.findByOidcIdTokenValue(token)

            tokenType == OAuth2TokenType.ACCESS_TOKEN -> repository.findByAccessTokenValue(token)
            tokenType == OAuth2TokenType.REFRESH_TOKEN -> repository.findByRefreshTokenValue(token)
            tokenType.value == "authorization_code" -> repository.findByAuthorizationCodeValue(token)
            tokenType.value == "id_token" -> repository.findByOidcIdTokenValue(token)
            else -> null
        }
        return doc?.let {
            fromOAuth2AuthorizationDocument(
                doc = it,
                registeredClient = registeredClientRepository.findById(it.registeredClientId) ?: return null,
                objectMapper = objectMapper
            )
        }
    }
}
