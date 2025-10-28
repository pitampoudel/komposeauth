package pitampoudel.core.domain

import io.ktor.util.encodeBase64
import pitampoudel.core.data.EncodedData

data class KmpFile(val byteArray: ByteArray, val mimeType: String, val name: String) {
    fun extension() =
        name.substringAfterLast('.', "").let { ext -> if (ext.isNotBlank()) ".$ext" else "" }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KmpFile

        return byteArray.contentEquals(other.byteArray)
    }

    override fun hashCode(): Int {
        return byteArray.contentHashCode()
    }

    fun toEncodedData() = EncodedData(
        base64EncodedData = byteArray.encodeBase64(),
        mimeType = mimeType,
        name = name
    )
}