package pitampoudel.komposeauth.core.utils

fun ByteArray.detectMimeType(): String = when {
    this.size >= 2 && this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte() -> "image/jpeg"
    this.size >= 8 && this[0] == 0x89.toByte() && this[1] == 0x50.toByte() -> "image/png"
    else -> "image/jpeg"
}