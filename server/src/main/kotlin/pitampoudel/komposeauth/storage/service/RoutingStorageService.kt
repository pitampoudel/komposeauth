package pitampoudel.komposeauth.storage.service

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * The [StorageService] everything is handed: new files go to the primary store on the Storage page,
 * and a read or a delete goes to whichever store the address belongs to.
 *
 * A file stays where it was written, under that store's address, until a move copies it: then the
 * old store still recognises the address and reads the bytes from the one they were copied to, with
 * no credential for its own cloud.
 */
@Primary
@Service
class RoutingStorageService(
    private val stores: StorageStores
) : StorageService {

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String {
        val primary = stores.primary() ?: error(
            if (stores.all().isEmpty()) "No storage is configured: add a bucket on the admin console's Storage page"
            else "No store takes new files: choose one on the admin console's Storage page"
        )
        return primary.store.upload(blobName, contentType, bytes)
    }

    override fun delete(url: String): Boolean {
        val located = stores.locate(url) ?: return false
        return located.reader.store.delete(located.name)
    }

    override fun blobNameOf(url: String): String? = stores.locate(url)?.name

    override fun download(url: String): ByteArray? {
        val located = stores.locate(url) ?: error("No store on the Storage page holds $url")
        return located.reader.store.download(located.name)
    }
}
