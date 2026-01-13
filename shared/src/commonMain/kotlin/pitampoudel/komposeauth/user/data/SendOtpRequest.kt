package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.komposeauth.user.domain.OtpType

@Serializable
data class SendOtpRequest(
    @SerialName("username")
    val username: String,
    @SerialName("type")
    val type: OtpType = OtpType.PHONE
)