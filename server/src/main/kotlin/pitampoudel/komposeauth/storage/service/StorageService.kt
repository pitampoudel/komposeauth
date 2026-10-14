package pitampoudel.komposeauth.storage.service

interface StorageService {
    fun upload(blobName: String, contentType: String?, bytes: ByteArray): String
    fun delete(url: String): Boolean

    /** The name a file at an address [upload] returned was stored under, or null when no store here holds it. */
    fun blobNameOf(url: String): String?

    /**
     * The file at an address [upload] returned, or null when the store it belongs to has nothing
     * there. Throws for an address no configured store holds: that is not the same as an absent file.
     */
    fun download(url: String): ByteArray?
}
