package pitampoudel.komposeauth.authorization

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class MongoOAuth2AuthorizationConsentServiceIntegrationTest {

    @Autowired
    private lateinit var consents: OAuth2AuthorizationConsentService

    private val client = "https://assistant.example/oauth/client-metadata"

    private fun consent(principal: String, vararg scopes: String) =
        OAuth2AuthorizationConsent.withId(client, principal).apply { scopes.forEach { scope(it) } }.build()

    private fun scopesOf(principal: String) =
        consents.findById(client, principal)?.scopes

    @Test
    fun `a consent is kept in Mongo, replaced when given again, and removed`() {
        assertIs<MongoOAuth2AuthorizationConsentService>(consents)
        val principal = ObjectId.get().toHexString()

        consents.save(consent(principal, "profile"))
        consents.save(consent(principal, "profile", "email"))
        assertEquals(setOf("profile", "email"), scopesOf(principal))
        assertNull(scopesOf(ObjectId.get().toHexString()), "another user's consent")

        consents.remove(consent(principal, "profile"))
        assertNull(scopesOf(principal))
    }
}
