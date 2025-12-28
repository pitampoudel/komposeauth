package pitampoudel.komposeauth.one_time_token.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "one_time_tokens")
@TypeAlias("one_time_token")
@CompoundIndex(
    def = "{'userId': 1, 'purpose': 1, 'expiresAt': 1}",
    name = "user_purpose_expiry_idx"
)
data class OneTimeToken(
    @Id
    val id: ObjectId? = null,
    @Indexed
    val userId: ObjectId,
    val purpose: Purpose,
    @Indexed(unique = true)
    val tokenHash: String,
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
    val consumedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
) {
    enum class Purpose {
        VERIFY_EMAIL,
        RESET_PASSWORD,
        REFRESH_TOKEN
    }

    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    fun isConsumed(): Boolean = consumedAt != null
    fun isValid(): Boolean = !isExpired() && !isConsumed()
}