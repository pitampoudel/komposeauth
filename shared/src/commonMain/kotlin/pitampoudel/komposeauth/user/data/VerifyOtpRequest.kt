package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyOtpRequest(
    @SerialName("otp")
    val otp: String
)