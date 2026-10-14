package pitampoudel.komposeauth.storage.service

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * The [StorageService] everything is handed: new files go to the primary bucket on the Storage page,
 * and a read or a delete goes to whichever bucket the address belongs to.
 *
 * A file stays where it was written, under that bucket's address. Once an admin has copied a bucket
 * elsewhere and said so on the Storage page, the old bucket still recognises the address and reads
 * the bytes from the copy, with no credential for its own cloud.
 */
@Primary
@Service
class RoutingStorageService(
    private val buckets: StorageBuckets
) : StorageService {

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String {
        val primary = buckets.primary() ?: error(
            if (buckets.all().isEmpty()) "No storage is configured: add a bucket on the admin console's Storage page"
            else "No bucket takes new files: choose one on the admin console's Storage page"
        )
        return primary.client.upload(blobName, contentType, bytes)
    }

    override fun delete(url: String): Boolean {
        val located = buckets.locate(url) ?: return false
        return located.reader.client.delete(located.name)
    }

    override fun blobNameOf(url: String): String? = buckets.locate(url)?.name

    override fun download(url: String): ByteArray? {
        val located = buckets.locate(url) ?: error("No bucket on the Storage page holds $url")
        return located.reader.client.download(located.name)
    }
}
