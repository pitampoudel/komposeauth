package pitampoudel.komposeauth.core.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import java.io.InputStream
import java.nio.channels.Channels

/**
 * [BucketStore] over a GCS bucket. Not a bean of its own: the storage page's stores are built into
 * one each, by [pitampoudel.komposeauth.storage.service.CloudBucketStores].
 *
 * The bucket has to exist; nothing here creates one.
 */
class GcpStorageService(val settings: Settings) : BucketStore {

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

    override fun list(prefix: String, after: String?, limit: Int): List<StoredObject> {
        val options = buildList {
            if (prefix.isNotEmpty()) add(Storage.BlobListOption.prefix(prefix))
            // Inclusive, unlike S3's startAfter, so the one named is skipped below.
            after?.let { add(Storage.BlobListOption.startOffset(it)) }
            add(Storage.BlobListOption.pageSize(limit + 1L))
        }
        return storage.list(settings.bucket, *options.toTypedArray()).values
            .asSequence()
            .filter { it.name != after }
            .take(limit)
            .map { StoredObject(it.name, it.size ?: 0L, it.updateTimeOffsetDateTime?.toInstant()) }
            .toList()
    }

    override fun stat(name: String): StoredObject? =
        storage.get(idOf(name))?.let { StoredObject(it.name, it.size ?: 0L, it.updateTimeOffsetDateTime?.toInstant()) }

    override fun <T> open(name: String, read: (OpenedObject) -> T): T? {
        val blob = storage.get(idOf(name)) ?: return null
        return storage.reader(blob.blobId).use { channel ->
            read(OpenedObject(Channels.newInputStream(channel), blob.size ?: 0L, blob.contentType))
        }
    }

    override fun write(name: String, contentType: String?, size: Long, content: InputStream) {
        val channel = storage.writer(BlobInfo.newBuilder(idOf(name)).setContentType(contentType).build())
        // Not `use`: closing the channel is what commits the upload, so a stream that fails part-way
        // has to leave it open rather than store the part that arrived.
        val written = content.copyTo(Channels.newOutputStream(channel))
        check(written == size) { "Read $written of the $size bytes of $name" }
        channel.close()
    }
}
