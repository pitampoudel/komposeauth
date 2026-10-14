package pitampoudel.komposeauth.core.migration

import org.springframework.data.mongodb.core.MongoTemplate

/**
 * Work [MongoMigrationRunner] does at startup and records in `schema_migrations`. Only
 * [MongoIndexMigration] is one: its id changes whenever an index annotation does, so it runs again.
 * A change to stored data is a Flamingock change instead (see `FlamingockConfig`).
 */
interface MongoMigration {
    val id: String
    val order: Int

    fun migrate(mongoTemplate: MongoTemplate)
}
