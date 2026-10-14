package pitampoudel.komposeauth.core.service

import java.io.InputStream
import java.time.Instant

/**
 * One bucket, reached by object name. [RoutingStorageService] turns an address on record into a
 * store and a name, so nothing here reads an address; [BucketAddresses] does that, with no client.
 */
interface BucketStore {
    /** Stores [bytes] under [name] and answers the address to keep on record. */
    fun upload(name: String, contentType: String?, bytes: ByteArray): String

    fun download(name: String): ByteArray?

    /** True once nothing is stored under [name], whether or not anything was. */
    fun delete(name: String): Boolean

    /** Up to [limit] objects whose names start with [prefix], in name order, after [after] when given. */
    fun list(prefix: String = "", after: String? = null, limit: Int): List<StoredObject>

    fun stat(name: String): StoredObject?

    /** Runs [read] over the object as it streams in, or answers null when there is no such object. */
    fun <T> open(name: String, read: (OpenedObject) -> T): T?

    /**
     * Stores the [size] bytes [content] streams under [name] without holding them all at once. A
     * stream that ends early or fails stores nothing.
     */
    fun write(name: String, contentType: String?, size: Long, content: InputStream)
}

data class StoredObject(val name: String, val size: Long, val updatedAt: Instant?)

class OpenedObject(val content: InputStream, val size: Long, val contentType: String?)
