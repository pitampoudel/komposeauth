package pitampoudel.komposeauth.core.service

interface StorageService {
    fun upload(blobName: String, contentType: String?, bytes: ByteArray): String
    fun download(blobName: String): ByteArray?
    fun exists(blobName: String): Boolean
    fun delete(url: String): Boolean
}