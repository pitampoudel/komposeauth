package pitampoudel.komposeauth.core.migrations

import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class DbMigrationRunner(
    private val mongoTemplate: MongoTemplate,
    val migrations: List<DbMigration>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun migrateIfNeeded() {
        val ownerId = createOwnerId()

        acquireLockOrThrow(ownerId)
        try {
            ensureSchemaStateExists()

            while (true) {
                val startingVersion = getCurrentVersion()
                logger.info("Current DB schema version: $startingVersion")

                val requiredMigrations = migrations
                    .filter { it.fromSchemaVersion == startingVersion }
                    .sortedWith(compareBy<DbMigration> { it.fromSchemaVersion }.thenBy { it.migrationId })

                if (requiredMigrations.isEmpty()) {
                    logger.info("No migrations needed. DB is up-to-date at version $startingVersion")
                    return
                }

                requiredMigrations.forEach { migration ->
                    heartbeatBestEffort(ownerId)

                    if (isMigrationAlreadyApplied(migration.migrationId)) {
                        logger.info("Skipping already-applied migration '${migration.migrationId}'")
                        return@forEach
                    }

                    val runId = UUID.randomUUID().toString()
                    startMigrationRun(runId, migration, ownerId)

                    logger.info("Running DB migration '${migration.migrationId}' from v${migration.fromSchemaVersion}")

                    try {
                        migration.run(mongoTemplate)
                        markMigrationSuccess(runId)
                    } catch (e: Exception) {
                        markMigrationFailure(runId, e)
                        throw e
                    }
                }

                val endingVersion = startingVersion + 1
                bumpVersionCas(expectedOldVersion = startingVersion, newVersion = endingVersion)

                logger.info("DB migrations complete. DB at version $endingVersion")
            }
        } finally {
            releaseLockBestEffort(ownerId)
        }
    }

    private fun ensureSchemaStateExists() {
        val exists = mongoTemplate.exists(
            Query.query(Criteria.where("_id").`is`("singleton")),
            SchemaState::class.java
        )
        if (exists) return

        try {
            mongoTemplate.insert(SchemaState(version = 0))
        } catch (_: DuplicateKeyException) {
            // Another instance created it.
        }
    }

    private fun getCurrentVersion(): Int {
        return mongoTemplate.findById("singleton", SchemaState::class.java)?.version ?: 0
    }

    private fun bumpVersionCas(expectedOldVersion: Int, newVersion: Int) {
        val query = Query.query(
            Criteria.where("_id").`is`("singleton")
                .and("version").`is`(expectedOldVersion)
        )
        val update = Update()
            .set("version", newVersion)
            .set("updatedAt", Instant.now())

        val result = mongoTemplate.updateFirst(query, update, SchemaState::class.java)
        if (result.matchedCount != 1L) {
            throw IllegalStateException(
                "SchemaState changed while migrating (expected version=$expectedOldVersion). " +
                        "Refusing to bump to $newVersion."
            )
        }
    }

    private fun isMigrationAlreadyApplied(migrationId: String): Boolean {
        val query = Query.query(
            Criteria.where("migrationId").`is`(migrationId)
                .and("status").`is`(MigrationRun.Status.SUCCESS)
        )
        return mongoTemplate.exists(query, MigrationRun::class.java)
    }

    private fun startMigrationRun(runId: String, migration: DbMigration, ownerId: String) {
        mongoTemplate.insert(
            MigrationRun(
                id = runId,
                migrationId = migration.migrationId,
                fromSchemaVersion = migration.fromSchemaVersion,
                startedAt = Instant.now(),
                status = MigrationRun.Status.RUNNING,
                ownerId = ownerId
            )
        )
    }

    private fun markMigrationSuccess(runId: String) {
        val query = Query.query(Criteria.where("_id").`is`(runId))
        val update = Update()
            .set("status", MigrationRun.Status.SUCCESS)
            .set("finishedAt", Instant.now())

        mongoTemplate.updateFirst(query, update, MigrationRun::class.java)
    }

    private fun markMigrationFailure(runId: String, e: Exception) {
        val query = Query.query(Criteria.where("_id").`is`(runId))
        val update = Update()
            .set("status", MigrationRun.Status.FAILED)
            .set("finishedAt", Instant.now())
            .set("errorMessage", (e.message ?: e.javaClass.name).take(8_000))

        mongoTemplate.updateFirst(query, update, MigrationRun::class.java)
    }

    /**
     * Distributed lock with a renewable lease.
     *
     * The lock document lives in the `locks` collection.
     */
    private fun acquireLockOrThrow(ownerId: String) {
        val now = Instant.now()
        val expiresAt = now.plus(LOCK_LEASE)

        val query = Query.query(
            Criteria.where("_id").`is`(LOCK_ID)
                .andOperator(
                    Criteria().orOperator(
                        Criteria.where("expiresAt").lt(now),
                        Criteria.where("expiresAt").exists(false)
                    )
                )
        )

        val update = Update()
            .set("_id", LOCK_ID)
            .set("ownerId", ownerId)
            .set("lockedAt", now)
            .set("heartbeatAt", now)
            .set("expiresAt", expiresAt)

        val options = FindAndModifyOptions.options().upsert(true).returnNew(true)

        val result = mongoTemplate.findAndModify(query, update, options, MigrationLock::class.java)
        if (result == null) {
            throw IllegalStateException("Could not acquire DB migration lock (another instance is migrating)")
        }
        logger.info("Acquired DB migration lock as ownerId=$ownerId")
    }

    private fun heartbeatBestEffort(ownerId: String) {
        try {
            val now = Instant.now()
            val query = Query.query(
                Criteria.where("_id").`is`(LOCK_ID)
                    .and("ownerId").`is`(ownerId)
            )
            val update = Update()
                .set("heartbeatAt", now)
                .set("expiresAt", now.plus(LOCK_LEASE))
            mongoTemplate.updateFirst(query, update, MigrationLock::class.java)
        } catch (e: Exception) {
            logger.warn("Failed to heartbeat DB migration lock (best-effort): ${e.message}")
        }
    }

    private fun releaseLockBestEffort(ownerId: String) {
        try {
            val query = Query.query(
                Criteria.where("_id").`is`(LOCK_ID)
                    .and("ownerId").`is`(ownerId)
            )
            mongoTemplate.remove(query, MigrationLock::class.java)
        } catch (e: Exception) {
            logger.warn("Failed to release DB migration lock (best-effort): ${e.message}")
        }
    }

    private fun createOwnerId(): String {
        val host = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            "unknown-host"
        }
        return "$host-${UUID.randomUUID()}"
    }

    companion object {
        internal const val LOCK_ID = "db_migration_lock"
        private val LOCK_LEASE: Duration = Duration.ofSeconds(30)
    }
}
