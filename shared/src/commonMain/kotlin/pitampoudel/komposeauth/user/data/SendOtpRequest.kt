package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    @SerialName("username")
    val username: String
)