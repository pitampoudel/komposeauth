package pitampoudel.komposeauth.organization

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
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
import pitampoudel.komposeauth.organization.migration._0001__OrganizationsFromDecember2025Change
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFails

/** An organization created in its first two weeks reads again, with the same times and address. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class OrganizationsFromDecember2025IntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val id = ObjectId()

    private fun organization() = mongoTemplate.findById(id, Organization::class.java)

    @AfterEach
    fun tearDown() {
        mongoTemplate.getCollection("organizations").deleteOne(Filters.eq("_id", id))
    }

    private fun kotlinInstant(instant: Instant) =
        Document("epochSeconds", instant.epochSecond).append("nanosecondsOfSecond", instant.nano)

    @Test
    fun `times kept as kotlin Instant become dates and the address moves to its line`() {
        val created = Instant.parse("2025-12-17T10:15:30.123456789Z")
        val updated = Instant.parse("2025-12-18T08:00:00Z")
        mongoTemplate.insert(
            Organization(
                id = id, name = "Org $id", email = "$id@example.com", logoUrl = null, country = "NP", state = null,
                city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null,
                description = null, website = null, userIds = listOf(ObjectId())
            )
        )
        mongoTemplate.getCollection("organizations").updateOne(
            Filters.eq("_id", id),
            Updates.combine(
                Updates.set("createdAt", kotlinInstant(created)),
                Updates.set("updatedAt", kotlinInstant(updated)),
                Updates.set("address", "Putalisadak, Kathmandu"),
                Updates.unset("addressLine1")
            )
        )
        assertFails("an organization in the old shape can't be read") { organization() }

        val change = _0001__OrganizationsFromDecember2025Change()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        val organization = organization()!!
        assertEquals(Instant.parse("2025-12-17T10:15:30.123Z"), organization.createdAt, "to the millisecond a date holds")
        assertEquals(updated, organization.updatedAt)
        assertEquals("Putalisadak, Kathmandu", organization.addressLine1)
        val stored = mongoTemplate.getCollection("organizations").find(Filters.eq("_id", id)).first()!!
        assertEquals(false, stored.containsKey("address"))
    }
}
