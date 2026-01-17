package pitampoudel.komposeauth.core.migration

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MongoMigrationRunner(
    private val mongoTemplate: MongoTemplate,
    private val migrations: List<MongoMigration>
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(MongoMigrationRunner::class.java)

    override fun run(args: ApplicationArguments) {
        if (migrations.isEmpty()) {
            return
        }

        migrations
            .sortedWith(compareBy<MongoMigration> { it.order }.thenBy { it.id })
            .forEach { migration ->
                if (!reserveMigration(migration)) {
                    logger.info("Skipping Mongo migration {} (already applied or reserved)", migration.id)
                    return@forEach
                }

                logger.info("Running Mongo migration {}", migration.id)
                try {
                    migration.migrate(mongoTemplate)
                } catch (ex: Exception) {
                    logger.error("Mongo migration {} failed; releasing reservation", migration.id, ex)
                    mongoTemplate.remove(
                        Query(Criteria.where("id").`is`(migration.id)),
                        SchemaMigration.COLLECTION_NAME
                    )
                    throw ex
                }
            }
    }

    private fun reserveMigration(migration: MongoMigration): Boolean {
        return try {
            mongoTemplate.insert(
                SchemaMigration(migration.id, Instant.now()),
                SchemaMigration.COLLECTION_NAME
            )
            true
        } catch (ex: DuplicateKeyException) {
            false
        }
    }
}
