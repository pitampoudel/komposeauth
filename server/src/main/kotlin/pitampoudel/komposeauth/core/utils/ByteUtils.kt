package pitampoudel.komposeauth.core.utils

/**
 * The raster image type these bytes start like, or null for anything else. Read off the bytes and
 * never off what an uploader said the file was, since it decides whether a browser is allowed to
 * render the file on this server's origin.
 */
fun ByteArray.imageMimeTypeOrNull(): String? = when {
    hasAt(0, 0xFF, 0xD8, 0xFF) -> "image/jpeg"
    hasAt(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "image/png"
    hasAt(0, *"GIF8".codes()) -> "image/gif"
    hasAt(0, *"RIFF".codes()) && hasAt(8, *"WEBP".codes()) -> "image/webp"
    else -> null
}

private fun String.codes(): IntArray = map { it.code }.toIntArray()

private fun ByteArray.hasAt(offset: Int, vararg expected: Int): Boolean =
    size >= offset + expected.size && expected.indices.all { this[offset + it] == expected[it].toByte() }
