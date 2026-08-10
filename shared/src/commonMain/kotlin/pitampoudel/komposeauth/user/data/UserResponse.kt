package pitampoudel.komposeauth.user.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UserResponse(
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val emailVerified: Boolean,
    val photoUrl: String?,
    @Contextual
    val createdAt: Instant,
    @Contextual
    val updatedAt: Instant,
    val phoneNumber: String?,
    val phoneNumberVerified: Boolean,
    val kycVerified: Boolean,
    /**
     * Always encoded, even when empty. The server's [kotlinx.serialization.json.Json] leaves
     * `encodeDefaults` off, so without this the field vanishes from the response for every user
     * who holds no role — and a client reading it gets `undefined` rather than an empty list.
     */
    @EncodeDefault
    val roles: List<String> = emptyList(),
) {
    init {
        require(email != null || phoneNumber != null)
    }
    fun verifiedPhoneNumber() = if (phoneNumberVerified) phoneNumber else null
    fun fullName() = "$firstName ${lastName ?: ""}"
}
