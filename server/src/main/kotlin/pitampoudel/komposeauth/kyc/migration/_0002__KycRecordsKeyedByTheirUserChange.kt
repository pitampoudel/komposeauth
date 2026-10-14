package pitampoudel.komposeauth.kyc.migration

import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.bson.Document
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig
import pitampoudel.komposeauth.kyc.entity.KycVerification

/**
 * Keys every KYC record by the user it belongs to, as records have been read since October 2025.
 *
 * Until then a record had an id of its own and named its user in `userId`. Nothing converted the ones
 * already stored, so each has been invisible to its owner and to the review screen since: an approved
 * user reads as unverified, and their document number, held by the old record, refuses a new
 * submission. The unique index the old records had on `userId` is dropped too where it is still
 * there, since it lets only one record without that field exist.
 *
 * Every such record is first copied to `kyc_verifications_legacy` and taken out of the collection,
 * then put back under its user's id where it still reads as a record and the user has no newer one.
 * The rest stay in the legacy collection, untouched, for a person to look at: a record from before
 * September 2025 that lacks what a record needs now, one naming a document type that no longer
 * exists, or one the user has since replaced.
 */
@Change(id = "kyc-records-keyed-by-their-user", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0002__KycRecordsKeyedByTheirUserChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: a record is set aside before it is removed, and only put back once. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val records = mongoTemplate.getCollection(KYC)
        val legacy = mongoTemplate.getCollection(LEGACY)

        records.listIndexes()
            .filter { (it["key"] as Document).keys == setOf(USER_ID) }
            .forEach { records.dropIndex(it.getString("name")) }

        records.find(Filters.exists(USER_ID)).toList().forEach { old ->
            legacy.replaceOne(Filters.eq("_id", old["_id"]), old, ReplaceOptions().upsert(true))
            records.deleteOne(Filters.eq("_id", old["_id"]))
        }

        var restored = 0
        val kept = mutableListOf<Any>()
        legacy.find(Filters.exists(USER_ID)).toList().forEach { old ->
            val userId = old[USER_ID] as? ObjectId
            val record = Document(old).apply { remove(USER_ID); put("_id", userId) }
            val restorable = userId != null &&
                records.find(Filters.eq("_id", userId)).first() == null &&
                runCatching { mongoTemplate.converter.read(KycVerification::class.java, record) }.isSuccess &&
                try {
                    records.insertOne(record)
                    true
                } catch (_: MongoWriteException) {
                    false
                }
            if (restorable) {
                legacy.deleteOne(Filters.eq("_id", old["_id"]))
                restored++
            } else {
                kept += old["_id"]!!
            }
        }
        if (restored > 0) log.info("Keyed {} KYC records by their user", restored)
        if (kept.isNotEmpty()) {
            log.warn("{} KYC records from before October 2025 stay in {} for a person to look at: {}", kept.size, LEGACY, kept)
        }
    }

    /** Nothing to undo: every record set aside is kept as it was stored. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val KYC = "kyc_verifications"
        const val LEGACY = "kyc_verifications_legacy"
        const val USER_ID = "userId"
    }
}
