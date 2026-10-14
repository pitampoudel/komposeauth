package pitampoudel.komposeauth.kyc

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
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.migration._0001__KycDatesStoredAsDatesChange
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFails

/** A record whose dates are still kept as `{value: <date>}` reads again, with the same days. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class KycDatesStoredAsDatesIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val userId = ObjectId()

    private fun record() = mongoTemplate.findById(userId, KycVerification::class.java)

    @AfterEach
    fun tearDown() {
        mongoTemplate.getCollection("kyc_verifications").deleteOne(Filters.eq("_id", userId))
    }

    private fun kotlinxDate(day: String) =
        Document("value", Date.from(LocalDate.parse(day).atStartOfDay(ZoneOffset.UTC).toInstant()))

    @Test
    fun `dates kept as kotlinx LocalDate are stored as dates`() {
        mongoTemplate.insert(
            KycVerification(
                userId = userId, country = "NP", nationality = "NP", firstName = "A", middleName = null,
                lastName = "B", dateOfBirth = LocalDate.parse("1990-01-01"), gender = KycResponse.Gender.MALE,
                fatherName = null, grandFatherName = null, maritalStatus = null,
                documentIssuedDate = LocalDate.parse("2015-06-15"), documentExpiryDate = LocalDate.parse("2030-06-14")
            )
        )
        mongoTemplate.getCollection("kyc_verifications").updateOne(
            Filters.eq("_id", userId),
            Updates.combine(
                Updates.set("dateOfBirth", kotlinxDate("1990-01-01")),
                Updates.set("documentIssuedDate", kotlinxDate("2015-06-15")),
                Updates.set("documentExpiryDate", kotlinxDate("2030-06-14"))
            )
        )
        assertFails("a record in the old shape can't be read") { record() }

        val change = _0001__KycDatesStoredAsDatesChange()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        val record = record()!!
        assertEquals(LocalDate.parse("1990-01-01"), record.dateOfBirth)
        assertEquals(LocalDate.parse("2015-06-15"), record.documentIssuedDate)
        assertEquals(LocalDate.parse("2030-06-14"), record.documentExpiryDate)
    }
}
