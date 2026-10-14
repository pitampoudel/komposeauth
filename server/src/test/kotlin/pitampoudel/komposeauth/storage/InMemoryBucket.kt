package pitampoudel.komposeauth.storage

import pitampoudel.komposeauth.storage.service.BucketAddresses
import pitampoudel.komposeauth.storage.service.BucketStore
import pitampoudel.komposeauth.storage.service.OpenedObject
import pitampoudel.komposeauth.storage.service.StoredObject
import java.io.InputStream
import java.time.Instant
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicInteger

/** A bucket kept in memory, handing out [addresses]' addresses, so a test can look at what is stored. */
class InMemoryBucket(
    private val addresses: BucketAddresses,
    private val now: () -> Instant = Instant::now
) : BucketStore {
    class Stored(val bytes: ByteArray, val contentType: String?, val updatedAt: Instant)

    val files = TreeMap<String, Stored>()
    val writes = mutableMapOf<String, AtomicInteger>()

    /** Names whose reading throws, as a file the server can't reach would. */
    val unreadable = mutableSetOf<String>()

    /** Runs before every listing, so a test can act in the middle of a slice. */
    var onList: () -> Unit = {}

    fun put(name: String, content: String, updatedAt: Instant = now()) {
        files[name] = Stored(content.toByteArray(), "text/plain", updatedAt)
    }

    fun text(name: String): String? = files[name]?.bytes?.decodeToString()

    @Synchronized
    override fun upload(name: String, contentType: String?, bytes: ByteArray): String {
        files[name] = Stored(bytes, contentType, now())
        writes.getOrPut(name, ::AtomicInteger).incrementAndGet()
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
    override fun list(prefix: String, after: String?, limit: Int): List<StoredObject> {
        onList()
        return files.entries.asSequence()
            .filter { it.key.startsWith(prefix) && (after == null || it.key > after) }
            .take(limit)
            .map { StoredObject(it.key, it.value.bytes.size.toLong(), it.value.updatedAt) }
            .toList()
    }

    @Synchronized
    override fun stat(name: String): StoredObject? =
        files[name]?.let { StoredObject(name, it.bytes.size.toLong(), it.updatedAt) }

    override fun <T> open(name: String, read: (OpenedObject) -> T): T? {
        if (name in unreadable) error("Access denied reading $name")
        val stored = synchronized(this) { files[name] } ?: return null
        return read(OpenedObject(stored.bytes.inputStream(), stored.bytes.size.toLong(), stored.contentType))
    }

    override fun write(name: String, contentType: String?, size: Long, content: InputStream) {
        val bytes = content.readBytes()
        check(bytes.size.toLong() == size) { "Expected $size bytes and read ${bytes.size}" }
        synchronized(this) {
            files[name] = Stored(bytes, contentType, now())
            writes.getOrPut(name, ::AtomicInteger).incrementAndGet()
        }
    }
}
