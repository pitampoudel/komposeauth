package pitampoudel.komposeauth.organization.migration

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
 * Brings the organizations created in their first two weeks, 17 to 30 December 2025, to the shape
 * they are read in.
 *
 * Their `createdAt` and `updatedAt` were `kotlin.time.Instant`, stored as
 * `{epochSeconds, nanosecondsOfSecond}`, which a `java.time.Instant` can't be read from: such an
 * organization can't be opened, edited or deleted, and every member's list of organizations fails.
 * And those created on the first two days kept their address in `address`, which became
 * `addressLine1` and is read from there.
 */
@Change(id = "organizations-from-december-2025", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__OrganizationsFromDecember2025Change {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: a date already stored as one, and an address already moved, are not matched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val organizations = mongoTemplate.getCollection(ORGANIZATIONS)
        val dates = listOf("createdAt", "updatedAt").sumOf { field ->
            organizations.updateMany(
                Filters.exists("$field.epochSeconds"),
                listOf(Document("\$set", Document(field, Document("\$toDate", millisOf(field)))))
            ).modifiedCount
        }
        val addresses = organizations.updateMany(
            Filters.and(Filters.exists("address"), Filters.exists("addressLine1", false)),
            listOf(
                Document("\$set", Document("addressLine1", "\$address")),
                Document("\$unset", "address")
            )
        ).modifiedCount
        if (dates + addresses > 0) {
            log.info("Converted {} organization dates and moved {} addresses", dates, addresses)
        }
    }

    /** `{epochSeconds, nanosecondsOfSecond}` as milliseconds since the epoch, which is what a date holds. */
    private fun millisOf(field: String) = Document(
        "\$add", listOf(
            Document("\$multiply", listOf("\$$field.epochSeconds", 1000L)),
            Document("\$trunc", Document("\$divide", listOf(Document("\$ifNull", listOf("\$$field.nanosecondsOfSecond", 0)), 1_000_000)))
        )
    )

    /** Nothing to undo: nothing reads either old shape any more. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val ORGANIZATIONS = "organizations"
    }
}
