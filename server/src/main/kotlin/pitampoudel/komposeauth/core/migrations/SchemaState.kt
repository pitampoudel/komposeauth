package pitampoudel.komposeauth.core.migrations

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("schema_state")
@TypeAlias("schema_state")
data class SchemaState(
    @Id
    val id: String = "singleton",
    val version: Int,
    val updatedAt: Instant = Instant.now()
)
