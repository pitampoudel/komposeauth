package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.komposeauth.user.domain.OtpType

@Serializable
data class VerifyOtpRequest(
    @SerialName("otp")
    val otp: String,
    @SerialName("type")
    val type: OtpType
)