package pitampoudel.komposeauth.core.migration

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = SchemaMigration.COLLECTION_NAME)
@TypeAlias("schema_migration")
data class SchemaMigration(
    @Id
    val id: String,
    val executedAt: Instant
) {
    companion object {
        const val COLLECTION_NAME = "schema_migrations"
    }
}
