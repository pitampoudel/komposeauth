package pitampoudel.komposeauth.storage

import pitampoudel.komposeauth.storage.service.BucketAddresses
import pitampoudel.komposeauth.storage.service.BucketClient
import pitampoudel.komposeauth.storage.service.StoredObject
import java.time.Instant
import java.util.TreeMap

/** A bucket kept in memory, handing out [addresses]' addresses, so a test can look at what is stored. */
class InMemoryBucket(
    private val addresses: BucketAddresses,
    private val now: () -> Instant = Instant::now
) : BucketClient {
    class Stored(val bytes: ByteArray, val contentType: String?, val updatedAt: Instant)

    val files = TreeMap<String, Stored>()

    fun put(name: String, content: String, updatedAt: Instant = now()) {
        files[name] = Stored(content.toByteArray(), "text/plain", updatedAt)
    }

    fun text(name: String): String? = files[name]?.bytes?.decodeToString()

    @Synchronized
    override fun upload(name: String, contentType: String?, bytes: ByteArray): String {
        files[name] = Stored(bytes, contentType, now())
        return addresses.addressOf(name)
    }

    @Synchronized
    override fun download(name: String): ByteArray? = files[name]?.bytes

    @Synchronized
    override fun delete(name: String): Boolean {
        files.remove(name)
        return true
    }

    @Synchronized
    override fun stat(name: String): StoredObject? =
        files[name]?.let { StoredObject(name, it.bytes.size.toLong(), it.updatedAt) }
}
