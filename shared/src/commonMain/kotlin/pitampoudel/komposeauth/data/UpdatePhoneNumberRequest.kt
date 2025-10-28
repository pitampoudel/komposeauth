package pitampoudel.komposeauth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePhoneNumberRequest(
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("countryCode")
    val countryCode: String? = null
)