package pitampoudel.komposeauth.kyc.migration

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig

/**
 * Returns to draft a KYC record that is waiting for review with no documents in it.
 *
 * Until October 2025 saving the personal details alone made a record wait for review, and since
 * then a record waiting for review can't take documents, so its owner can never send them. Every way
 * a record comes to wait for review now stores the three documents first, so one without any was
 * never submitted.
 */
@Change(id = "unsubmitted-kyc-back-to-draft", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0003__UnsubmittedKycBackToDraftChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: a draft is not matched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val drafts = mongoTemplate.getCollection(KYC).updateMany(
            Filters.and(
                Filters.eq("status", "PENDING"),
                Filters.eq("documentFrontUrl", null),
                Filters.eq("documentBackUrl", null),
                Filters.eq("selfieUrl", null)
            ),
            Updates.set("status", "DRAFT")
        ).modifiedCount
        if (drafts > 0) log.info("{} KYC records waiting for review with no documents are drafts again", drafts)
    }

    /** Nothing to undo: no record without documents can be reviewed. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val KYC = "kyc_verifications"
    }
}
