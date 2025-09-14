package com.vardansoft.authx

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import org.bson.Document
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

/**
 * Migration component to update User entities with RemoteFile picture fields to use URL strings instead.
 * This migration extracts the publicUrl from the RemoteFile object and updates the user with the extracted URL.
 */
@Component
@Profile("!test") // Don't run in test profile
class MongoDataMigration(
    private val mongoTemplate: MongoTemplate
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
    }

    /**
     * Migrates all users with RemoteFile picture fields to use URL strings instead.
     */
    fun migrateUserPictures() {
        println("Starting migration of User picture fields from RemoteFile to URL strings...")

        val usersCollection = mongoTemplate.getCollection("users")

        // Find all users with a non-null picture field
        val usersWithPicture = usersCollection.find(Filters.exists("picture"))

        var updatedCount = 0

        usersWithPicture.forEach { user ->
            val picture = user.get("picture")

            // Check if picture is a Document (representing a RemoteFile object)
            if (picture is Document) {
                val publicUrl = picture.getString("publicUrl")

                if (publicUrl != null) {
                    // Update the user with the extracted URL
                    usersCollection.updateOne(
                        Filters.eq("_id", user.getObjectId("_id")),
                        Updates.set("picture", publicUrl)
                    )

                    updatedCount++
                }
            }
        }

        println("Migration completed. Updated $updatedCount user(s)")
    }
}
