package pitampoudel.komposeauth.oauth_clients

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.oauth_clients.migration._0001__RegisteredClientsAreFirstPartyChange
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A client the console registered before February 2026 is read as this platform's own again. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class RegisteredClientsAreFirstPartyIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var registeredClients: RegisteredClientRepository

    private val clients get() = mongoTemplate.getCollection("oauth2_clients")
    private val created = mutableListOf<Any>()

    /**
     * As the console saved one before February 2026: a secret, 'none' beside it, and consent. An id
     * that reads as an ObjectId is stored as one, as Spring Data stores it.
     */
    private fun client(id: Any, consent: Boolean): Any {
        clients.insertOne(
            Document("_id", id)
                .append("clientSecret", "secret")
                .append("clientName", "App $id")
                .append("clientAuthenticationMethods", listOf(Document("value", "client_secret_post"), Document("value", "none")))
                .append("authorizationGrantTypes", listOf(Document("value", "authorization_code"), Document("value", "refresh_token")))
                .append("redirectUris", listOf("https://app.example/callback"))
                .append("scopes", listOf("openid", "profile", "email"))
                .append("requireAuthorizationConsent", consent)
        )
        created += id
        return id
    }

    private fun consentOf(id: Any) = clients.find(Filters.eq("_id", id)).first()!!.getBoolean("requireAuthorizationConsent")

    @AfterEach
    fun tearDown() {
        clients.deleteMany(Filters.`in`("_id", created))
    }

    @Test
    fun `clients saved asking for consent no longer do, and swagger-ui is deleted`() {
        val old = ObjectId()
        client(old, consent = true)
        val new = client(ObjectId(), consent = false)
        val swaggerUi = client("swagger-ui", consent = true)
        assertTrue(registeredClients.findByClientId(old.toHexString())!!.clientSettings.isRequireAuthorizationConsent)

        val change = _0001__RegisteredClientsAreFirstPartyChange()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        assertFalse(consentOf(old))
        assertFalse(consentOf(new))
        assertNull(clients.find(Filters.eq("_id", swaggerUi)).first())
        assertFalse(registeredClients.findByClientId(old.toHexString())!!.clientSettings.isRequireAuthorizationConsent)
    }
}
