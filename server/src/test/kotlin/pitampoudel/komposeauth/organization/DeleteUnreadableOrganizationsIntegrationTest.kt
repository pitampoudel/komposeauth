package pitampoudel.komposeauth.organization

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.organization.migration._0001__DeleteUnreadableOrganizationsChange
import kotlin.test.assertEquals

/** An organization created in December 2025's last two weeks is deleted, and every other one kept. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class DeleteUnreadableOrganizationsIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val organizations get() = mongoTemplate.getCollection("organizations")
    private val created = mutableListOf<ObjectId>()

    /** An organization as stored, edited into an older shape. */
    private fun stored(edit: Document.() -> Unit): ObjectId {
        val id = ObjectId()
        val organization = Organization(
            id = id, name = "Org $id", email = "$id@example.com", logoUrl = null, country = "NP", state = null,
            city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null,
            description = null, website = null, userIds = listOf(ObjectId())
        )
        organizations.insertOne(Document().also { mongoTemplate.converter.write(organization, it) }.apply(edit))
        created += id
        return id
    }

    @AfterEach
    fun tearDown() {
        organizations.deleteMany(Filters.`in`("_id", created))
    }

    @Test
    fun `an organization with kotlin Instant times is deleted`() {
        val current = stored {}
        val december = stored {
            put("createdAt", Document("epochSeconds", 1_765_966_530L).append("nanosecondsOfSecond", 0))
            put("address", "Putalisadak, Kathmandu")
        }

        val change = _0001__DeleteUnreadableOrganizationsChange()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        val remaining = organizations.find(Filters.`in`("_id", created)).map { it.getObjectId("_id") }.toSet()
        assertEquals(setOf(current), remaining, "$december goes")
    }
}
