package com.vardansoft.core.data

import com.vardansoft.core.domain.KmpFile
import io.ktor.util.decodeBase64Bytes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EncodedData(
    @SerialName("base64EncodedData")
    val base64EncodedData: String,
    @SerialName("mime_type")
    val mimeType: String
) {
    fun toKmpFile() = KmpFile(base64EncodedData.decodeBase64Bytes(), mimeType)
}