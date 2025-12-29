package pitampoudel.komposeauth.user.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class UserResponse(
    val id: String,
    val firstName: String,
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
) {
    init {
        require(email != null || phoneNumber != null)
    }
    fun verifiedPhoneNumber() = if (phoneNumberVerified) phoneNumber else null
    fun fullName() = "$firstName ${lastName ?: ""}"
}
