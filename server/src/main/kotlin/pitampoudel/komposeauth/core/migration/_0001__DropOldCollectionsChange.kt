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
 * Drops the collections nothing reads any more: `env`, where the settings lived until November 2025
 * (some of them secrets, in plaintext), and the records of the server's two earlier migration
 * runners (`schema_migrations`, `schema_state`, `locks`). Flamingock keeps its own.
 */
@Change(id = "drop-old-collections", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__DropOldCollectionsChange {

    /** Safe to run again: dropping a collection that is not there does nothing. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        COLLECTIONS.forEach(mongoTemplate::dropCollection)
    }

    /** Nothing to undo: the release before creates its indexes again when it finds no record of them. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        val COLLECTIONS = listOf("env", "schema_migrations", "schema_state", "locks")
    }
}
