package pitampoudel.komposeauth.storage.service

import java.time.Instant

/**
 * One bucket, reached by object name. [RoutingStorageService] turns an address on record into a
 * bucket and a name, so nothing here reads an address; [BucketAddresses] does that, with no client.
 */
interface BucketClient {
    /** Stores [bytes] under [name] and answers the address to keep on record. */
    fun upload(name: String, contentType: String?, bytes: ByteArray): String

    fun download(name: String): ByteArray?

    /** True once nothing is stored under [name], whether or not anything was. */
    fun delete(name: String): Boolean

    fun stat(name: String): StoredObject?
}

data class StoredObject(val name: String, val size: Long, val updatedAt: Instant?)
