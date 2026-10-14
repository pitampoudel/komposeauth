package pitampoudel.komposeauth.kyc

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
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
import pitampoudel.komposeauth.kyc.migration._0002__KycRecordsKeyedByTheirUserChange
import pitampoudel.komposeauth.kyc.migration._0003__UnsubmittedKycBackToDraftChange
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** A record from before October 2025 reaches its owner and the review screen again. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class KycRecordsFromBeforeOctober2025IntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val records get() = mongoTemplate.getCollection("kyc_verifications")
    private val legacy get() = mongoTemplate.getCollection("kyc_verifications_legacy")
    private val created = mutableListOf<ObjectId>()

    private fun record(
        userId: ObjectId,
        firstName: String = "A",
        documentNumber: String? = null,
        status: KycResponse.Status = KycResponse.Status.DRAFT,
        documentUrl: String? = null
    ) = KycVerification(
        userId = userId, country = "NP", nationality = "NP", firstName = firstName, middleName = null,
        lastName = "B", dateOfBirth = LocalDate.parse("1990-01-01"), gender = KycResponse.Gender.MALE,
        fatherName = null, grandFatherName = null, maritalStatus = null, documentNumber = documentNumber,
        documentFrontUrl = documentUrl, documentBackUrl = documentUrl, selfieUrl = documentUrl, status = status
    ).also { created += userId }

    /** As the releases before October 2025 stored one: an id of its own, and its user in `userId`. */
    private fun old(record: KycVerification, edit: Document.() -> Unit = {}): ObjectId {
        val id = ObjectId().also { created += it }
        val stored = Document().also { mongoTemplate.converter.write(record, it) }
            .apply { put("_id", id); put("userId", record.userId); edit() }
        records.insertOne(stored)
        return id
    }

    private fun recordOf(userId: ObjectId) = mongoTemplate.findById(userId, KycVerification::class.java)

    @AfterEach
    fun tearDown() {
        records.deleteMany(Filters.`in`("_id", created))
        legacy.deleteMany(Filters.`in`("_id", created))
    }

    @Test
    fun `a record keyed by an id of its own is keyed by its user`() {
        val user = ObjectId()
        val oldId = old(record(user, documentNumber = "DOC-$user"))
        records.createIndex(Indexes.ascending("userId"))
        assertNull(recordOf(user), "the owner's record is looked up by their id")

        val change = _0002__KycRecordsKeyedByTheirUserChange()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        assertEquals("DOC-$user", recordOf(user)?.documentNumber)
        assertNull(records.find(Filters.eq("_id", oldId)).first())
        assertNull(legacy.find(Filters.eq("_id", oldId)).first())
        assertFalse(records.listIndexes().any { (it["key"] as Document).keys == setOf("userId") })
    }

    @Test
    fun `a record its user has replaced, or that no longer reads, is set aside`() {
        val replacing = ObjectId()
        mongoTemplate.insert(record(replacing, firstName = "New"))
        val replaced = old(record(replacing, firstName = "Old"))
        val unreadable = old(record(ObjectId())) { remove("firstName") }

        val change = _0002__KycRecordsKeyedByTheirUserChange()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        assertEquals("New", recordOf(replacing)?.firstName)
        assertEquals(
            setOf(replaced, unreadable),
            legacy.find(Filters.`in`("_id", replaced, unreadable)).map { it.getObjectId("_id") }.toSet()
        )
        assertEquals(0, records.countDocuments(Filters.`in`("_id", replaced, unreadable)))
    }

    @Test
    fun `a record waiting for review with no documents is a draft again`() {
        val bare = ObjectId()
        val sent = ObjectId()
        mongoTemplate.insert(record(bare, status = KycResponse.Status.PENDING))
        mongoTemplate.insert(record(sent, status = KycResponse.Status.PENDING, documentUrl = "kyc/$sent"))

        _0003__UnsubmittedKycBackToDraftChange().apply(mongoTemplate)

        assertEquals(KycResponse.Status.DRAFT, recordOf(bare)?.status)
        assertEquals(KycResponse.Status.PENDING, recordOf(sent)?.status)
    }
}
