package pitampoudel.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.domain.KmpFile
import kotlin.io.encoding.Base64

@Serializable
data class EncodedData(
    @SerialName("base64EncodedData")
    val base64EncodedData: String,
    @SerialName("mimeType")
    val mimeType: String,
    @SerialName("name")
    val name: String
) {
    fun toKmpFile() = KmpFile(
        byteArray = Base64.decode(base64EncodedData),
        mimeType = mimeType,
        name = name
    )
}