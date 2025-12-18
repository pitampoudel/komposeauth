package pitampoudel.komposeauth.organization.data


import pitampoudel.core.data.PhoneNumber
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.komposeauth.core.data.AddressInformation
import kotlin.time.Instant

@Serializable
data class OrganizationResponse(
    @SerialName("address")
    val address: AddressInformation,
    @SerialName("createdAt")
    @Contextual  val createdAt: Instant,
    @SerialName("description")
    val description: String?,
    @SerialName("email")
    val email: String,
    @SerialName("emailVerified")
    val emailVerified: Boolean,
    @SerialName("id")
    val id: String,
    @SerialName("logoUrl")
    val logoUrl: String?,
    @SerialName("name")
    val name: String,
    @SerialName("phoneNumber")
    val phoneNumber: PhoneNumber?,
    @SerialName("phoneNumberVerified")
    val phoneNumberVerified: Boolean,
    @SerialName("registrationNo")
    val registrationNo: String?,
    @SerialName("socialLinks")
    val socialLinks: List<String>,
    @SerialName("website")
    val website: String?
)
