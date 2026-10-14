package pitampoudel.komposeauth.kyc

import com.mongodb.client.model.Filters
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.migration._0001__DropOldMigrationRecordsChange
import pitampoudel.komposeauth.kyc.migration._0004__KeepKycSealingRecordChange
import pitampoudel.komposeauth.kyc.service.KycDocumentSealing
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The old runner's collections go, and the record that the KYC files were sealed is kept. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class KeepKycSealingRecordIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Test
    fun `the sealing record outlives the collections it was kept in`() {
        val sealedAt = Date.from(Instant.parse("2026-09-20T05:00:00Z"))
        mongoTemplate.getCollection("schema_migrations").insertOne(
            Document("_id", KycDocumentSealing.ID).append("executedAt", sealedAt)
        )
        mongoTemplate.getCollection("schema_state").insertOne(Document("_id", "state"))
        mongoTemplate.getCollection("locks").insertOne(Document("_id", "migration"))

        repeat(2) {
            _0004__KeepKycSealingRecordChange().apply(mongoTemplate)
            _0001__DropOldMigrationRecordsChange().apply(mongoTemplate)
        }

        val record = mongoTemplate.getCollection(KycDocumentSealing.COLLECTION)
            .find(Filters.eq("_id", KycDocumentSealing.ID)).first()
        assertEquals(sealedAt, record?.getDate("sealedAt"))
        val left = mongoTemplate.collectionNames
        assertTrue(listOf("schema_migrations", "schema_state", "locks").none { it in left }, "left: $left")
    }
}
