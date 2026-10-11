package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The body of a sign-out; the whole body is optional, and signing out without one still works. */
@Serializable
data class LogoutRequest(
    /** The refresh token this device holds, which stops working along with every one it was refreshed from. */
    @SerialName("refreshToken")
    val refreshToken: String? = null
)
