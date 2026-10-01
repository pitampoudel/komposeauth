package pitampoudel.komposeauth.oauth_clients.repository

import pitampoudel.komposeauth.oauth_clients.cimd.ClientIdMetadataDocuments
import pitampoudel.komposeauth.oauth_clients.dto.toRegisteredClient
import pitampoudel.komposeauth.oauth_clients.entity.OAuth2Client
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository


@Repository
interface OAuth2ClientRepository : MongoRepository<OAuth2Client, String>

@Component
class RegisteredOAuth2ClientRepository(
    val repository: OAuth2ClientRepository,
    private val metadataDocuments: ClientIdMetadataDocuments,
) : RegisteredClientRepository {
    override fun save(registeredClient: RegisteredClient?) {
        // No-op implementation as we don't need to save clients from the authorization server
        // Clients are managed through the admin API
    }

    override fun findById(id: String): RegisteredClient? {
        // A client identified by its metadata document URL has that URL as its id too
        if (metadataDocuments.isDocumentUrl(id)) return metadataDocuments.find(id)
        return repository.findById(id)
            .map { it.toRegisteredClient() }
            .orElse(null)
    }

    override fun findByClientId(clientId: String): RegisteredClient? {
        if (metadataDocuments.isDocumentUrl(clientId)) return metadataDocuments.find(clientId)
        return repository.findById(clientId)
            .map { it.toRegisteredClient() }
            .orElse(null)
    }


}
