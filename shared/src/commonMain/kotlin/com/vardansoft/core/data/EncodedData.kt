package com.vardansoft.core.data

import com.vardansoft.core.domain.KmpFile
import io.ktor.util.decodeBase64Bytes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EncodedData(
    @SerialName("data")
    val data: String,
    @SerialName("mime_type")
    val mimeType: String
) {
    fun toKmpFile() = KmpFile(data.decodeBase64Bytes(), mimeType)
}