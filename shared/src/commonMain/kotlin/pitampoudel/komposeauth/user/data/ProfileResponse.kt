package pitampoudel.komposeauth.user.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProfileResponse(
    @SerialName("givenName")
    val givenName: String?,
    @SerialName("familyName")
    val familyName: String?,
    @SerialName("email")
    val email: String,
    @SerialName("phoneNumber")
    val phoneNumber: String?,
    @SerialName("emailVerified")
    val emailVerified: Boolean,
    @SerialName("phoneNumberVerified")
    val phoneNumberVerified: Boolean,
    /**
     * Always encoded, even when false. The server's [kotlinx.serialization.json.Json] leaves
     * `encodeDefaults` off, so without this the field vanishes for every user who has not passed
     * KYC — and a client cannot tell "not verified" apart from "the server did not say".
     */
    @EncodeDefault
    @SerialName("kycVerified")
    val kycVerified: Boolean = false,
    @SerialName("picture")
    val picture: String?,
    @SerialName("id")
    val id: String,
    @SerialName("createdAt")
    @Contextual val createdAt: Instant,
    @SerialName("updatedAt")
    @Contextual val updatedAt: Instant,
    @SerialName("socialLinks")
    val socialLinks: List<String>,
    @SerialName("roles")
    val roles: List<String>
) {
    fun fullName() = "$givenName $familyName"
}