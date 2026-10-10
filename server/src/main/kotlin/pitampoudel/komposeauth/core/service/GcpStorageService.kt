package pitampoudel.komposeauth.core.service

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.BucketInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions

/**
 * [StorageService] over a GCS bucket. Not a bean of its own: [RoutingStorageService] builds one for
 * whatever bucket the config names.
 */
class GcpStorageService(val settings: Settings) : StorageService {

    data class Settings(val projectId: String?, val bucket: String)

    private val storage: Storage by lazy {
        StorageOptions.newBuilder().setProjectId(settings.projectId).build().service
    }

    private val bucket by lazy {
        storage.get(settings.bucket) ?: storage.create(BucketInfo.newBuilder(settings.bucket).build())
    }

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String {
        val precondition: Bucket.BlobTargetOption = if (bucket.get(blobName) == null)
            Bucket.BlobTargetOption.doesNotExist()
        else
            Bucket.BlobTargetOption.generationMatch(bucket.get(blobName).generation)
        val blob = bucket.create(blobName, bytes, contentType, precondition)
        return blob.mediaLink
    }

    /** True when [url] is an address in this bucket. */
    fun owns(url: String): Boolean = blobNameOf(url) != null

    override fun delete(url: String): Boolean {
        val blobName = blobNameOf(url) ?: return false
        val blob = bucket.get(blobName)
        return blob?.delete() ?: true
    }

    private fun blobNameOf(url: String): String? {
        // Example: https://storage.googleapis.com/download/storage/v1/b/bucket-name/o/blob-name?generation=123&alt=media
        val mediaLinkRegex =
            Regex("https://storage\\.googleapis\\.com/download/storage/v1/b/([^/]+)/o/([^?]+)")
        mediaLinkRegex.find(url)?.let { match ->
            if (match.groupValues[1] != settings.bucket) return null
            return java.net.URLDecoder.decode(match.groupValues[2], "UTF-8")
        }

        // Example: gs://bucket-name/blob-name or https://storage.googleapis.com/bucket-name/blob-name
        val directUrlRegex = Regex("(?:gs://|https://storage\\.googleapis\\.com/)([^/]+)/(.+)")
        directUrlRegex.find(url)?.let { match ->
            if (match.groupValues[1] != settings.bucket) return null
            return match.groupValues[2]
        }

        return null
    }
}
