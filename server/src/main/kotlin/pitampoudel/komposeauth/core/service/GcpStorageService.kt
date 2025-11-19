package pitampoudel.komposeauth.core.service

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.BucketInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.AppProperties


@Service
class GcpStorageService(
    val appProperties: AppProperties
) : StorageService {
    private val storage: Storage by lazy {
        StorageOptions.newBuilder().setProjectId(appProperties.gcpProjectId).build().service
    }

    private val bucket by lazy {
        storage.get(appProperties.gcpBucketName) ?: storage.create(
            BucketInfo.newBuilder(appProperties.gcpBucketName).build()
        )
    }

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String {
        val precondition: Bucket.BlobTargetOption = if (bucket.get(blobName) == null)
            Bucket.BlobTargetOption.doesNotExist()
        else
            Bucket.BlobTargetOption.generationMatch(bucket.get(blobName).generation)
        val blob = bucket.create(blobName, bytes, contentType, precondition)
        return blob.mediaLink
    }

    override fun download(blobName: String): ByteArray? {
        val blob = bucket.get(blobName)
        return blob?.getContent()
    }

    override fun exists(blobName: String): Boolean {
        return bucket.get(blobName) != null
    }

    override fun delete(url: String): Boolean {
        val blobName = extractBlobName(url) ?: return false
        val blob = bucket.get(blobName)
        return blob?.delete() ?: true
    }

    private fun extractBlobName(url: String?): String? {
        if (url == null) return null

        // Check if it's a GCP Storage URL (mediaLink format)
        // Example: https://storage.googleapis.com/download/storage/v1/b/bucket-name/o/blob-name?generation=123&alt=media
        val mediaLinkRegex =
            Regex("https://storage\\.googleapis\\.com/download/storage/v1/b/([^/]+)/o/([^?]+)")
        val mediaLinkMatch = mediaLinkRegex.find(url)

        if (mediaLinkMatch != null) {
            val blobName = mediaLinkMatch.groupValues[2]
            return java.net.URLDecoder.decode(blobName, "UTF-8")
        }

        // Check if it's a direct GCP Storage URL
        // Example: gs://bucket-name/blob-name or https://storage.googleapis.com/bucket-name/blob-name
        val directUrlRegex = Regex("(?:gs://|https://storage\\.googleapis\\.com/)([^/]+)/(.+)")
        val directUrlMatch = directUrlRegex.find(url)

        if (directUrlMatch != null) {
            return directUrlMatch.groupValues[2]
        }

        return null
    }
}
