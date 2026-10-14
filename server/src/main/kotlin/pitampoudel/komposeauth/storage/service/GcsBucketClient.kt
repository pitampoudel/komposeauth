package pitampoudel.komposeauth.storage.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions

/**
 * [BucketClient] over a GCS bucket. Not a bean of its own: each bucket on the Storage page gets one,
 * built by [pitampoudel.komposeauth.storage.service.CloudBucketClients].
 *
 * The bucket has to exist; nothing here creates one.
 */
class GcsBucketClient(val settings: Settings) : BucketClient {

    data class Settings(
        val projectId: String?,
        val bucket: String,
        val publicBaseUrl: String? = null,
        /** A service account key; null signs in as the host's own identity. */
        val serviceAccountJson: String? = null
    ) {
        override fun toString() =
            "Settings(projectId=$projectId, bucket=$bucket, publicBaseUrl=$publicBaseUrl, serviceAccountKey=${serviceAccountJson != null})"
    }

    val addresses = GcsAddresses(settings.bucket, settings.publicBaseUrl)

    private val storage: Storage by lazy {
        StorageOptions.newBuilder()
            .setProjectId(settings.projectId)
            .apply {
                settings.serviceAccountJson?.let { setCredentials(GoogleCredentials.fromStream(it.byteInputStream())) }
            }
            .build().service
    }

    private fun idOf(name: String) = BlobId.of(settings.bucket, name)

    /**
     * The media link, whose generation changes with every write, so a browser holding the last photo
     * under the same name fetches the new one; behind a public base, its address with the generation.
     */
    override fun upload(name: String, contentType: String?, bytes: ByteArray): String {
        val id = idOf(name)
        val precondition = storage.get(id)?.let { Storage.BlobTargetOption.generationMatch(it.generation) }
            ?: Storage.BlobTargetOption.doesNotExist()
        val blob = storage.create(BlobInfo.newBuilder(id).setContentType(contentType).build(), bytes, precondition)
        return if (settings.publicBaseUrl != null) "${addresses.addressOf(name)}?v=${blob.generation}" else blob.mediaLink
    }

    override fun download(name: String): ByteArray? = storage.get(idOf(name))?.getContent()

    override fun delete(name: String): Boolean {
        storage.delete(idOf(name))
        return true
    }

    override fun stat(name: String): StoredObject? =
        storage.get(idOf(name))?.let { StoredObject(it.name, it.size ?: 0L, it.updateTimeOffsetDateTime?.toInstant()) }
}
