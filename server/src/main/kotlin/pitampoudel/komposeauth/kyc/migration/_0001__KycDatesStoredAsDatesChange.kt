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

/**
 * Stores a KYC record's dates as dates, which is how they have been read since January 2026.
 *
 * Until then they were `kotlinx.datetime.LocalDate`, kept as `{value: <date>}`. The migration written
 * for it on 1 January converted the date of birth and the issue date, and the expiry date was added
 * to it the same afternoon under the same id, so a database it had already reached never had expiry
 * dates converted. A record still holding one can't be read: its owner's sign-in fails, since a token
 * says whether they are verified, and so does the admin's list of records awaiting review.
 */
@Change(id = "kyc-dates-stored-as-dates", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__KycDatesStoredAsDatesChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: a date already stored as one is not matched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val records = mongoTemplate.getCollection(KYC)
        val converted = DATES.sumOf { field ->
            records.updateMany(
                Filters.type("$field.value", "date"),
                listOf(Document("\$set", Document(field, "\$$field.value")))
            ).modifiedCount
        }
        if (converted > 0) log.info("Converted {} KYC dates kept in the old shape", converted)
    }

    /** Nothing to undo: nothing reads a date in the old shape any more. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val KYC = "kyc_verifications"
        val DATES = listOf("dateOfBirth", "documentIssuedDate", "documentExpiryDate")
    }
}
