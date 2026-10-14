package pitampoudel.komposeauth.kyc.migration

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig
import pitampoudel.komposeauth.kyc.service.KycDocumentSealing
import java.util.Date

/**
 * Copies the record that the KYC files stored before sealing are all sealed from `schema_migrations`,
 * where the release before kept it, to `kyc_document_sealing`, where [KycDocumentSealing] reads it now.
 * Without it every file in the bucket is read and rewritten once more on the next start.
 */
@Change(id = "keep-kyc-sealing-record", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0004__KeepKycSealingRecordChange {

    /** Safe to run again: the copy replaces itself. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val sealed = mongoTemplate.getCollection(OLD_RECORDS)
            .find(Filters.eq("_id", KycDocumentSealing.ID))
            .first() ?: return
        mongoTemplate.getCollection(KycDocumentSealing.COLLECTION).replaceOne(
            Filters.eq("_id", KycDocumentSealing.ID),
            Document("_id", KycDocumentSealing.ID).append("sealedAt", sealed.getDate("executedAt") ?: Date()),
            ReplaceOptions().upsert(true)
        )
    }

    /** Nothing to undo: the release before reads its own copy. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val OLD_RECORDS = "schema_migrations"
    }
}
