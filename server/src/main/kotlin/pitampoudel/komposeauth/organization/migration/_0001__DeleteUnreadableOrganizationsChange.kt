package pitampoudel.komposeauth.organization.migration

import com.mongodb.client.model.Filters
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig
import pitampoudel.komposeauth.organization.entity.Organization

/**
 * Deletes the organizations created in the last two weeks of December 2025, which nothing has been
 * able to read since: their times were kept as kotlin `Instant` (`{epochSeconds,
 * nanosecondsOfSecond}`) and their address in `address`.
 */
@Change(id = "delete-unreadable-organizations", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__DeleteUnreadableOrganizationsChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: an organization that reads is never matched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val organizations = mongoTemplate.getCollection(ORGANIZATIONS)
        val unreadable = organizations.find().filter {
            runCatching { mongoTemplate.converter.read(Organization::class.java, it) }.isFailure
        }.map { it["_id"] }.toList()
        if (unreadable.isEmpty()) return
        organizations.deleteMany(Filters.`in`("_id", unreadable))
        log.warn("Deleted {} organizations from December 2025 that could not be read: {}", unreadable.size, unreadable)
    }

    /** Nothing to undo: no release since January 2026 could read these. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val ORGANIZATIONS = "organizations"
    }
}
