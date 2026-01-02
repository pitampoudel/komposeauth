package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("countryCode")
    val countryCode: String? = null
)