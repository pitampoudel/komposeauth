package pitampoudel.komposeauth.core.service

interface StorageService {
    fun upload(blobName: String, contentType: String?, bytes: ByteArray): String
    fun delete(url: String): Boolean
}
