package pitampoudel.komposeauth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyPhoneOtpRequest(
    @SerialName("countryCode")
    val countryCode: String?,
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("otp")
    val otp: String
)