package pitampoudel.core.data

import io.ktor.util.decodeBase64Bytes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.domain.KmpFile

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
        byteArray = base64EncodedData.decodeBase64Bytes(),
        mimeType = mimeType,
        name = name
    )
}