package pitampoudel.komposeauth.core.migration

import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig

/**
 * Drops the collections the server's two earlier migration runners kept their records in: the one
 * from January 2026 (`schema_migrations`, `schema_state`, `locks`) and the one that replaced it, which
 * reused `schema_migrations` and last recorded only index runs. Flamingock keeps its own. Its stage
 * runs last, after the KYC stage has copied the one record still read from them.
 */
@Change(id = "drop-old-migration-records", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__DropOldMigrationRecordsChange {

    /** Safe to run again: dropping a collection that is not there does nothing. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        COLLECTIONS.forEach(mongoTemplate::dropCollection)
    }

    /** Nothing to undo: the release before creates its indexes again when it finds no record of them. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        val COLLECTIONS = listOf("schema_migrations", "schema_state", "locks")
    }
}
