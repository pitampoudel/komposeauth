package pitampoudel.komposeauth.kyc.service

import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.core.migration.SchemaMigration
import pitampoudel.komposeauth.kyc.entity.KycDocument
import pitampoudel.komposeauth.kyc.entity.KycVerification
import java.time.Instant
import kotlin.concurrent.thread

/**
 * Seals the KYC files stored before [KycDocuments] sealed them, which until then anyone holding or
 * guessing their bucket address could read.
 *
 * Not a `MongoMigration`, though it is recorded as done in the same `schema_migrations`. A migration
 * holds back readiness while it runs and keeps its reservation if the process is stopped part-way;
 * this reads and rewrites every file in the bucket, which can outlast a health check, and would then
 * never run again. So it runs in the background once the server is up, and is recorded as done only
 * when every file is sealed or gone: a run that fails or is stopped is repeated on the next start.
 * Repeating it is safe, since a sealed file is left as it is and a record is pointed at the sealed
 * copy only while it still names the file that was sealed.
 */
@Component
class KycDocumentSealing(
    private val mongoTemplate: MongoTemplate,
    private val documents: KycDocuments
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val ID = "kyc-documents-sealed"
    }

    data class Outcome(val sealed: Int, val missing: List<String>, val failed: List<String>)

    @EventListener(ApplicationReadyEvent::class)
    fun sealInBackground() {
        thread(name = "kyc-document-sealing", isDaemon = true) {
            try {
                if (mongoTemplate.exists(Query(Criteria.where("_id").`is`(ID)), SchemaMigration.COLLECTION_NAME)) {
                    return@thread
                }
                if (sealStoredFiles().failed.isEmpty()) markDone()
            } catch (ex: Exception) {
                log.error("Could not seal the KYC files stored before sealing; trying again on the next start", ex)
            }
        }
    }

    fun sealStoredFiles(): Outcome {
        var sealed = 0
        val missing = mutableListOf<String>()
        val failed = mutableListOf<String>()
        // Just the addresses, read up front: a cursor held open across this many uploads could time out.
        val withFiles = Query(
            Criteria().orOperator(KycDocument.entries.map { Criteria.where(it.storedUrl.name).ne(null) })
        ).apply { KycDocument.entries.forEach { fields().include(it.storedUrl.name) } }
        val records = mongoTemplate.find(
            withFiles,
            Document::class.java,
            mongoTemplate.getCollectionName(KycVerification::class.java)
        )
        for (record in records) {
            val userId = record.getObjectId("_id")
            for (document in KycDocument.entries) {
                val storedUrl = record.getString(document.storedUrl.name) ?: continue
                val name = "${userId.toHexString()}/${document.label}"
                try {
                    when (val sealing = documents.sealStored(userId, document, storedUrl)) {
                        is KycDocuments.Sealing.Sealed -> {
                            // Matching nothing means the user sent new files meanwhile, which were
                            // sealed as they were stored.
                            mongoTemplate.updateFirst(
                                Query(Criteria.where("_id").`is`(userId).and(document.storedUrl.name).`is`(storedUrl)),
                                Update().set(document.storedUrl.name, sealing.storedUrl),
                                KycVerification::class.java
                            )
                            sealed++
                        }
                        KycDocuments.Sealing.AlreadySealed -> Unit
                        KycDocuments.Sealing.Missing -> missing += name
                    }
                } catch (ex: Exception) {
                    log.warn("Could not seal KYC file {}", name, ex)
                    failed += name
                }
            }
        }
        if (sealed > 0) log.info("Sealed {} KYC files that were stored before sealing", sealed)
        if (missing.isNotEmpty()) {
            log.warn("{} KYC files named on a record were not in the store, so there was nothing to seal: {}", missing.size, missing)
        }
        if (failed.isNotEmpty()) {
            log.error("{} KYC files are still stored unsealed and will be tried again on the next start: {}", failed.size, failed)
        }
        return Outcome(sealed, missing, failed)
    }

    private fun markDone() {
        try {
            mongoTemplate.insert(SchemaMigration(ID, Instant.now()), SchemaMigration.COLLECTION_NAME)
        } catch (_: DuplicateKeyException) {
            // Another instance finished the same work first.
        }
    }
}
