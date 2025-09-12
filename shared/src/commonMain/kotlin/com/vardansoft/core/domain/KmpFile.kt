package com.vardansoft.core.domain

import com.vardansoft.core.data.EncodedData
import io.ktor.util.encodeBase64

data class KmpFile(val byteArray: ByteArray, val mimeType: String) {
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
        data = byteArray.encodeBase64(),
        mimeType = mimeType
    )
}