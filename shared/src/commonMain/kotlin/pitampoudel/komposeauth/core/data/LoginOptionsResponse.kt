package pitampoudel.komposeauth.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginOptionsResponse(
    @SerialName("googleClientId")
    val googleClientId: String?,
    @SerialName("publicKeyAuthOptionsJson")
    val publicKeyAuthOptionsJson: String?
)
