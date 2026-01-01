package pitampoudel.komposeauth.core.migrations

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("locks")
@TypeAlias("migration_lock")
data class MigrationLock(
    @Id
    val id: String = "db_migration_lock",
    val ownerId: String,
    val lockedAt: Instant,
    val heartbeatAt: Instant,
    val expiresAt: Instant
)

