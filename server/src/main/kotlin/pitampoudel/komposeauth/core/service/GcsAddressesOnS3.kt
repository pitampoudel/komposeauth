package pitampoudel.komposeauth.core.service

/**
 * A GCS bucket's addresses, answered by the S3 bucket it was copied into under the same names
 * (deploy/aws/README.md). Stands in for [GcpStorageService] once `gcsFilesReadFrom` is "s3", and
 * builds no Google client, so a file written before the move is read and deleted with no Google
 * credential. Never where new files go: it only ever names an old address.
 */
class GcsAddressesOnS3(private val gcsBucket: String, private val copy: S3StorageService) : StorageService {

    /** A `..` segment is refused: keys are flat, and a name that climbs out of its folder is not ours. */
    private fun keyOf(url: String): String? =
        GcpStorageService.blobNameIn(gcsBucket, url)?.takeUnless { name -> name.split('/').any { it == ".." } }

    fun owns(url: String): Boolean = keyOf(url) != null

    override fun blobNameOf(url: String): String? = keyOf(url)

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String =
        copy.upload(blobName, contentType, bytes)

    override fun delete(url: String): Boolean {
        val key = keyOf(url) ?: return false
        return copy.deleteKey(key)
    }

    override fun download(url: String): ByteArray? {
        val key = keyOf(url) ?: throw IllegalArgumentException("$url is not in bucket $gcsBucket")
        return copy.downloadKey(key)
    }
}
