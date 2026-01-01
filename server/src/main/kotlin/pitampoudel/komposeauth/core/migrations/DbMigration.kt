package pitampoudel.komposeauth.core.migrations

import org.springframework.data.mongodb.core.MongoTemplate

interface DbMigration {
    val migrationId: String
    val fromSchemaVersion: Int
    fun run(mongoTemplate: MongoTemplate)
}
