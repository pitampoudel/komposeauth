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

        println("Migration completed. Updated $updatedCount user(s) with RemoteFile picture fields.")
    }

    /**
     * Migrates all users with SocialLink objects to use URL strings instead.
     */
    fun migrateSocialLinksToUrls() {
        println("Starting migration of User socialLinks fields from SocialLink objects to URL strings...")

        val usersCollection = mongoTemplate.getCollection("users")

        // Find all users with a non-empty socialLinks field
        val usersWithSocialLinks = usersCollection.find(
            Filters.and(
                Filters.exists("socialLinks"),
                Filters.ne("socialLinks", emptyList<Any>())
            )
        )

        var updatedCount = 0

        usersWithSocialLinks.forEach { user ->
            val socialLinks = user.get("socialLinks")

            // Check if socialLinks is a list of Documents (representing SocialLink objects)
            if (socialLinks is List<*>) {
                val urlList = mutableListOf<String>()
                var hasDocuments = false

                socialLinks.forEach { socialLink ->
                    if (socialLink is Document) {
                        hasDocuments = true
                        val url = socialLink.getString("url")
                        if (url != null) {
                            urlList.add(url)
                        }
                    } else if (socialLink is String) {
                        // Already a URL string, keep it
                        urlList.add(socialLink)
                    }
                }

                // Only update if we found Document objects (meaning it needs migration)
                if (hasDocuments) {
                    usersCollection.updateOne(
                        Filters.eq("_id", user.getObjectId("_id")),
                        Updates.set("socialLinks", urlList)
                    )

                    updatedCount++
                }
            }
        }

        println("Migration completed. Updated $updatedCount user(s) with SocialLink objects to URL strings.")
    }
}
