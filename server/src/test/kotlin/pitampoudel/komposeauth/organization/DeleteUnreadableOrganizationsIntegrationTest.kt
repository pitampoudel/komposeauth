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
import pitampoudel.komposeauth.kyc.KycFlowTestOverrides
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.organization.migration._0001__DeleteUnreadableOrganizationsChange
import kotlin.test.assertEquals

/**
 * An organization created in December 2025's last two weeks is deleted with its logo, and every
 * other one is kept, with the logo it has.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class DeleteUnreadableOrganizationsIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val organizations get() = mongoTemplate.getCollection("organizations")
    private val created = mutableListOf<ObjectId>()
    private val storage = KycFlowTestOverrides.InMemoryStorage()

    private fun logo(name: String) = storage.upload("organization_logos/$name", "image/png", byteArrayOf(1))

    /** An organization with [logoUrl] as stored, edited into an older shape. */
    private fun stored(logoUrl: String, edit: Document.() -> Unit): ObjectId {
        val id = ObjectId()
        val organization = Organization(
            id = id, name = "Org $id", email = "$id@example.com", logoUrl = logoUrl, country = "NP", state = null,
            city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null,
            description = null, website = null, userIds = listOf(ObjectId())
        )
        organizations.insertOne(Document().also { mongoTemplate.converter.write(organization, it) }.apply(edit))
        created += id
        return id
    }

    private fun Document.fromDecember2025() {
        put("createdAt", Document("epochSeconds", 1_765_966_530L).append("nanosecondsOfSecond", 0))
        put("address", "Putalisadak, Kathmandu")
    }

    @AfterEach
    fun tearDown() {
        organizations.deleteMany(Filters.`in`("_id", created))
    }

    @Test
    fun `an organization with kotlin Instant times is deleted with its logo`() {
        val shared = logo("1765966530")
        val current = stored(shared) {}
        val december = stored(logo("1765966531")) { fromDecember2025() }
        val sameSecond = stored(shared) { fromDecember2025() }

        val change = _0001__DeleteUnreadableOrganizationsChange()
        change.apply(mongoTemplate, storage)
        change.apply(mongoTemplate, storage)

        val remaining = organizations.find(Filters.`in`("_id", created)).map { it.getObjectId("_id") }.toSet()
        assertEquals(setOf(current), remaining, "$december and $sameSecond go")
        assertEquals(setOf(shared), storage.files.keys, "the logo the kept organization shows stays")
    }
}
