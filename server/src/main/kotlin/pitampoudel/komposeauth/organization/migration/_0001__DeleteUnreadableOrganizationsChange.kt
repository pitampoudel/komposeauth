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
import pitampoudel.komposeauth.storage.service.StorageService
import pitampoudel.komposeauth.storage.service.deleteUnsharedFiles

/**
 * Deletes the organizations created in the last two weeks of December 2025, which nothing has been
 * able to read since: their times were kept as kotlin `Instant` (`{epochSeconds,
 * nanosecondsOfSecond}`) and their address in `address`. Their logos go with them, unless an
 * organization that stays has the same file.
 */
@Change(id = "delete-unreadable-organizations", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__DeleteUnreadableOrganizationsChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Safe to run again: an organization that reads is never matched, and the logos go before the
     * organizations, so a run cut off between the two finds them again.
     */
    @Apply
    fun apply(mongoTemplate: MongoTemplate, storage: StorageService) {
        val organizations = mongoTemplate.getCollection(ORGANIZATIONS)
        val unreadable = organizations.find().filter {
            runCatching { mongoTemplate.converter.read(Organization::class.java, it) }.isFailure
        }.toList()
        if (unreadable.isEmpty()) return
        val ids = unreadable.map { it["_id"] }
        val kept = organizations.find(Filters.nin("_id", ids)).mapNotNull { it[LOGO] as? String }
        val left = storage.deleteUnsharedFiles(unreadable.mapNotNull { it[LOGO] as? String }, kept)
        organizations.deleteMany(Filters.`in`("_id", ids))
        log.warn("Deleted {} organizations from December 2025 that could not be read, and their logos: {}", ids.size, ids)
        if (left.isNotEmpty()) log.warn("Could not delete these logos of the deleted organizations: {}", left)
    }

    /** Nothing to undo: no release since January 2026 could read these. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val ORGANIZATIONS = "organizations"
        const val LOGO = "logoUrl"
    }
}
