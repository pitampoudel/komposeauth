package pitampoudel.komposeauth.authorization

import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.authorization.migration._0001__DeleteAbandonedConsentsChange
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals

/** A consent screen nobody answered, saved by the release before with no expiry, is deleted. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class DeleteAbandonedConsentsIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val created = mutableListOf<String>()

    private fun authorization(vararg fields: Pair<String, Any>): String {
        val id = ObjectId.get().toHexString()
        mongoTemplate.getCollection(COLLECTION).insertOne(
            Document("_id", id)
                .append("registeredClientId", "client")
                .append("principalName", "user")
                .append("authorizationGrantType", "authorization_code")
                .apply { fields.forEach { (name, value) -> append(name, value) } }
        )
        created += id
        return id
    }

    private fun stored(): Set<String> = mongoTemplate.getCollection(COLLECTION)
        .find(Document("_id", Document("\$in", created))).map { it.getString("_id") }.toSet()

    @AfterEach
    fun tearDown() {
        mongoTemplate.remove(Query(Criteria.where("_id").`in`(created)), COLLECTION)
    }

    @Test
    fun `only an authorization holding no token and no expiry is deleted`() {
        val abandoned = authorization("state" to "state-${ObjectId.get()}")
        val withCode = authorization("authorizationCodeValue" to "code-${ObjectId.get()}")
        val signedIn = authorization("refreshTokenValue" to "refresh-${ObjectId.get()}")
        val dated = authorization(
            "state" to "state-${ObjectId.get()}",
            "expiresAt" to Date.from(Instant.parse("2026-11-01T00:00:00Z"))
        )

        _0001__DeleteAbandonedConsentsChange().apply(mongoTemplate)

        assertEquals(setOf(withCode, signedIn, dated), stored(), "$abandoned goes, and only it")
    }

    private companion object {
        const val COLLECTION = "oauth2_authorizations"
    }
}
