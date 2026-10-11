package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.komposeauth.user.domain.OtpType

@Serializable
data class VerifyOtpRequest(
    @SerialName("username")
    val username: String,
    @SerialName("type")
    val type: OtpType,
    @SerialName("otp")
    val otp: String,
    /**
     * Needed only to replace an already verified email or phone number of an account that has a
     * password; see [pitampoudel.komposeauth.core.domain.ErrorCode.CURRENT_PASSWORD_REQUIRED].
     */
    @SerialName("currentPassword")
    val currentPassword: String? = null
)