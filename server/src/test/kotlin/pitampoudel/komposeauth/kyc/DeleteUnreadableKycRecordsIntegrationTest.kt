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
import pitampoudel.komposeauth.kyc.entity.KycDocument
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.migration._0001__DeleteUnreadableKycRecordsChange
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The KYC records saved before January 2026 are deleted with their files, and every other one is kept. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class DeleteUnreadableKycRecordsIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val records get() = mongoTemplate.getCollection("kyc_verifications")
    private val created = mutableListOf<ObjectId>()
    private val storage = KycFlowTestOverrides.InMemoryStorage()

    /** A record for [userId] naming its three files, which are stored as `kyc/<user id>/<label>`. */
    private fun record(userId: ObjectId = ObjectId()): KycVerification {
        val files = KycDocument.entries.associateWith {
            storage.upload("kyc/${userId.toHexString()}/${it.label}", null, byteArrayOf(1))
        }
        return KycVerification(
            userId = userId, country = "NP", nationality = "NP", firstName = "A", middleName = null,
            lastName = "B", dateOfBirth = LocalDate.parse("1990-01-01"), gender = KycResponse.Gender.MALE,
            fatherName = null, grandFatherName = null, maritalStatus = null
        ).copy(
            documentFrontUrl = files[KycDocument.FRONT],
            documentBackUrl = files[KycDocument.BACK],
            selfieUrl = files[KycDocument.SELFIE]
        )
    }

    /** [record] as stored, edited into an older shape. */
    private fun stored(record: KycVerification, edit: Document.() -> Unit): ObjectId {
        val document = Document().also { mongoTemplate.converter.write(record, it) }.apply(edit)
        records.insertOne(document)
        return document.getObjectId("_id").also { created += it }
    }

    /** As the records before October 2025 were kept: an id of their own and the user in `userId`. */
    private fun Document.keyedByOwnId() {
        put("userId", get("_id"))
        put("_id", ObjectId())
    }

    private fun kotlinxDate(day: String) =
        Document("value", Date.from(LocalDate.parse(day).atStartOfDay(ZoneOffset.UTC).toInstant()))

    private fun remaining() = records.find(Filters.`in`("_id", created)).map { it.getObjectId("_id") }.toSet()

    private fun filesOf(user: ObjectId) = KycDocument.entries.map { "test://kyc/${user.toHexString()}/${it.label}" }

    @AfterEach
    fun tearDown() {
        records.deleteMany(Filters.`in`("_id", created))
    }

    @Test
    fun `records kept with kotlinx dates or their own id are deleted with their files`() {
        val current = stored(record()) {}
        val kotlinxDates = stored(record()) { put("dateOfBirth", kotlinxDate("1990-01-01")) }
        val ownId = stored(record()) { keyedByOwnId() }
        records.createIndex(Indexes.ascending("userId"))

        val change = _0001__DeleteUnreadableKycRecordsChange()
        change.apply(mongoTemplate, storage)
        change.apply(mongoTemplate, storage)

        assertEquals(setOf(current), remaining(), "$kotlinxDates and $ownId go")
        assertEquals(filesOf(current).toSet(), storage.files.keys, "only the kept record's files are left")
        assertFalse(records.listIndexes().any { (it["key"] as Document).keys == setOf("userId") })
    }

    @Test
    fun `a file the owner's newer record names is kept`() {
        val user = ObjectId()
        val current = stored(record(user)) {}
        stored(record(user)) { keyedByOwnId() }

        _0001__DeleteUnreadableKycRecordsChange().apply(mongoTemplate, storage)

        assertEquals(setOf(current), remaining())
        assertEquals(filesOf(user).toSet(), storage.files.keys)
    }
}
