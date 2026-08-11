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
    /**
     * What this token proves the holder reached, as opposed to who it belongs to.
     *
     * [userId] alone says "whoever clicks this is the owner of that account", which is all a
     * password reset needs. A [Purpose.VERIFY_EMAIL] link claims something narrower — that somebody
     * read a message sent to one particular address — and the address has to be written down at
     * send time for the claim to survive until the click. Null on purposes that make no such claim,
     * and on links issued before this field existed.
     */
    val subject: String? = null,
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