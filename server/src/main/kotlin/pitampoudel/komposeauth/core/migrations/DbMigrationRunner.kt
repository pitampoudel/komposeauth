package pitampoudel.komposeauth.core.migrations

import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DbMigrationRunner(
    private val mongoTemplate: MongoTemplate,
    val migrations: List<DbMigration>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun migrateIfNeeded() {

        acquireLockOrThrow()
        try {
            ensureSchemaStateExists()

            val currentVersion = getCurrentVersion()
            logger.info("Current DB schema version: $currentVersion")

            val requiredMigrations = migrations.filter { it.fromSchemaVersion == currentVersion }
            if (requiredMigrations.isEmpty()) {
                logger.info("No migrations needed. DB is up-to-date at version $currentVersion")
                return
            }

            requiredMigrations.forEach { migration ->
                logger.info("Running DB migration from v${migration.fromSchemaVersion}")
                migration.run(mongoTemplate)
            }
            val newVersion = currentVersion + 1
            bumpVersion(newVersion)

            logger.info("DB migrations complete. DB at version $newVersion")
        } finally {
            releaseLockBestEffort()
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

    private fun bumpVersion(newVersion: Int) {
        val query = Query.query(Criteria.where("_id").`is`("singleton"))
        val update = Update()
            .set("version", newVersion)
            .set("updatedAt", Instant.now())

        mongoTemplate.upsert(query, update, SchemaState::class.java)
    }

    /**
     * Best-effort distributed lock using atomic upsert.
     */
    private fun acquireLockOrThrow() {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(5)

        val query = Query.query(
            Criteria.where("_id").`is`("db_migration_lock")
                .andOperator(
                    Criteria().orOperator(
                        Criteria.where("expiresAt").lt(now),
                        Criteria.where("expiresAt").exists(false)
                    )
                )
        )

        val update = Update()
            .set("_id", "db_migration_lock")
            .set("expiresAt", expiresAt)
            .set("lockedAt", now)

        val options = FindAndModifyOptions.options().upsert(true).returnNew(true)

        val result = mongoTemplate.findAndModify(query, update, options, Map::class.java, "locks")
        if (result == null) {
            throw IllegalStateException("Could not acquire DB migration lock (another instance is migrating)")
        }
    }

    private fun releaseLockBestEffort() {
        try {
            val query = Query.query(Criteria.where("_id").`is`("db_migration_lock"))
            mongoTemplate.remove(query, "locks")
        } catch (e: Exception) {
            logger.warn("Failed to release DB migration lock (best-effort): ${e.message}")
        }
    }
}
