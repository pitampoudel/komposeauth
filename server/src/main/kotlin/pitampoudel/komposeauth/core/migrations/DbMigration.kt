package pitampoudel.komposeauth.core.migrations

import org.springframework.data.mongodb.core.MongoTemplate

interface DbMigration {
    val fromSchemaVersion: Int
    fun run(mongoTemplate: MongoTemplate)
}
