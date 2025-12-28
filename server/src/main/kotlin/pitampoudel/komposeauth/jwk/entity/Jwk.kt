package pitampoudel.komposeauth.jwk.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("jwks")
@TypeAlias("jwk")
data class Jwk(
    @Id
    val id: ObjectId = ObjectId(),
    @Indexed(unique = true)
    val kid: String,
    // PEM encoded strings
    val publicKeyPem: String,
    val privateKeyPem: String,
    val createdAt: Instant = Instant.now()
)
