package pitampoudel.komposeauth.kyc.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UrlResponse(
    @SerialName("url")
    val url: String
)
