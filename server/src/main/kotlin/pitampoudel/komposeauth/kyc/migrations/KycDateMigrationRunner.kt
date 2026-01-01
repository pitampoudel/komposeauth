package pitampoudel.komposeauth.kyc.migrations

import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.core.migrations.DbMigration
import java.util.*

/**
 * Normalize KYC date fields from nested { value: ... } to Date
 */
@Component
class KycDateFieldMigration : DbMigration {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val migrationId: String = "2026-01-01-kyc-date-field"
    override val fromSchemaVersion: Int = 0

    override fun run(mongoTemplate: MongoTemplate) {
        logger.info("Starting KYC date field migration...")

        val collection = mongoTemplate.getCollection("kyc_verifications")

        val filter = Document(
            $$"$or", listOf(
                Document("dateOfBirth.value", Document($$"$exists", true)),
                Document("documentIssuedDate.value", Document($$"$exists", true))
            )
        )

        val documentsToMigrate = collection.find(filter).toList()
        logger.info("Found ${documentsToMigrate.size} documents to migrate")

        var successCount = 0
        var errorCount = 0

        documentsToMigrate.forEachIndexed { index, doc ->
            try {

                val updates = Document()
                var hasUpdate = false

                // Migrate dateOfBirth if needed
                val dateOfBirth = doc["dateOfBirth"]

                if (dateOfBirth is Document && dateOfBirth.containsKey("value")) {
                    val value = dateOfBirth["value"]

                    val dateValue = extractDateFromValue(value)
                    if (dateValue != null) {
                        updates["dateOfBirth"] = dateValue
                        hasUpdate = true
                    }
                }

                val documentIssuedDate = doc["documentIssuedDate"]

                if (documentIssuedDate is Document && documentIssuedDate.containsKey("value")) {
                    val value = documentIssuedDate["value"]

                    val dateValue = extractDateFromValue(value)
                    if (dateValue != null) {
                        updates["documentIssuedDate"] = dateValue
                        hasUpdate = true
                    }
                }

                if (hasUpdate) {
                    val updateDoc = Document($$"$set", updates)
                    collection.updateOne(
                        Document("_id", doc["_id"]),
                        updateDoc
                    )
                    successCount++
                }
            } catch (e: Exception) {
                logger.error("Error processing document ${doc["_id"]}: ${e.message}", e)
                errorCount++
            }
        }

        logger.info("Migration completed - Success: $successCount, Errors: $errorCount")
    }

    private fun extractDateFromValue(value: Any?): Date? {
        return when (value) {
            is Date -> value
            is Document -> {
                val dateField = value[$$"$date"]
                when (dateField) {
                    is Date -> dateField
                    is String -> Date.from(java.time.Instant.parse(dateField))
                    is Long -> Date(dateField)
                    is Number -> Date(dateField.toLong())
                    else -> null
                }
            }

            else -> null
        }
    }
}