package pitampoudel.komposeauth.oauth_clients.repository

import org.springframework.cache.annotation.Cacheable
import pitampoudel.komposeauth.core.config.CacheConfig.Companion.OAUTH2_CLIENTS_CACHE
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
class RegisteredOAuth2ClientRepository(val repository: OAuth2ClientRepository) : RegisteredClientRepository {
    override fun save(registeredClient: RegisteredClient?) {
        // No-op implementation as we don't need to save clients from the authorization server
        // Clients are managed through the admin API
    }

    @Cacheable(value = [OAUTH2_CLIENTS_CACHE], key = "#id")
    override fun findById(id: String): RegisteredClient {
        val client = repository.findById(id).orElseThrow { IllegalArgumentException("No client found with id: $id") }
        return client.toRegisteredClient()
    }

    @Cacheable(value = [OAUTH2_CLIENTS_CACHE], key = "#clientId")
    override fun findByClientId(clientId: String): RegisteredClient {
        val client = repository.findById(clientId)
            .orElseThrow { IllegalArgumentException("No client found with clientId: $clientId") }
        return client.toRegisteredClient()
    }


}
