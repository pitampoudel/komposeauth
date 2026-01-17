package pitampoudel.komposeauth.core.migration

import org.springframework.data.mongodb.core.MongoTemplate

interface MongoMigration {
    val id: String
    val order: Int

    fun migrate(mongoTemplate: MongoTemplate)
}
