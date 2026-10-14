package pitampoudel.komposeauth.kyc.migration

import com.mongodb.client.model.Filters
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig
import pitampoudel.komposeauth.kyc.entity.KycDocument
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.storage.service.StorageService
import pitampoudel.komposeauth.storage.service.deleteUnsharedFiles

/**
 * Deletes the KYC records saved before January 2026, which nothing has been able to read since.
 *
 * Their dates were kept as `{value: <date>}` by kotlinx-datetime, and the ones from before October
 * 2025 also had an id of their own with the user in `userId`, while every lookup is by the user's
 * id. Such a record shows its owner as unverified and, through its document number, refuses the new
 * submission that would fix it. The unique index those records had on `userId` is dropped too.
 *
 * Their owners verify again. Their scans and selfies go with them: nothing else names them, and the
 * ones stored before sealing can be read by anyone holding the address. A file the owner's newer
 * record also names stays, since the file names were by user even before the records were.
 */
@Change(id = "delete-unreadable-kyc-records", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__DeleteUnreadableKycRecordsChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Safe to run again: a record that reads is never matched, and the files go before the records,
     * so a run cut off between the two finds them again.
     */
    @Apply
    fun apply(mongoTemplate: MongoTemplate, storage: StorageService) {
        val records = mongoTemplate.getCollection(KYC)
        records.listIndexes()
            .filter { (it["key"] as Document).keys == setOf(USER_ID) }
            .forEach { records.dropIndex(it.getString("name")) }

        val unreadable = records.find().filter { record ->
            record.containsKey(USER_ID) ||
                runCatching { mongoTemplate.converter.read(KycVerification::class.java, record) }.isFailure
        }.toList()
        if (unreadable.isEmpty()) return
        val ids = unreadable.map { it["_id"] }
        val kept = records.find(Filters.nin("_id", ids)).flatMap(::filesOf)
        val left = storage.deleteUnsharedFiles(unreadable.flatMap(::filesOf), kept)
        records.deleteMany(Filters.`in`("_id", ids))
        log.warn("Deleted {} KYC records saved before January 2026, and their files", ids.size)
        if (left.isNotEmpty()) log.warn("Could not delete these files of the deleted KYC records: {}", left)
    }

    private fun filesOf(record: Document) = KycDocument.entries.mapNotNull { record[it.storedUrl.name] as? String }

    /** Nothing to undo: no release since January 2026 could read these. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val KYC = "kyc_verifications"
        const val USER_ID = "userId"
    }
}
