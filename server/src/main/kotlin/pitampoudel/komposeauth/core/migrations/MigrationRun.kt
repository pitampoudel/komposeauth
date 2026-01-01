package pitampoudel.komposeauth.core.migrations

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("schema_migrations")
@TypeAlias("migration_run")
data class MigrationRun(
    @Id
    val id: String,
    @Indexed(unique = true)
    val migrationId: String,
    val fromSchemaVersion: Int,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val status: Status,
    val ownerId: String,
    val errorMessage: String? = null
) {
    enum class Status {
        RUNNING,
        SUCCESS,
        FAILED
    }

}


