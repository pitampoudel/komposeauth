package pitampoudel.komposeauth.storage.service

import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.storage.entity.StorageBucket
import pitampoudel.komposeauth.storage.repository.StorageBucketRepository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/** Builds the client for a bucket from its decrypted row. A seam, so a test can keep a bucket in memory. */
fun interface BucketClientFactory {
    fun build(bucket: StorageBucket): BucketClient
}

@Component
class CloudBucketClients : BucketClientFactory {
    override fun build(bucket: StorageBucket): BucketClient = when (bucket.kind) {
        StorageBucket.Kind.GCS -> GcsBucketClient(
            GcsBucketClient.Settings(
                projectId = bucket.projectId,
                bucket = bucket.bucketName,
                publicBaseUrl = bucket.publicBaseUrl,
                serviceAccountJson = bucket.serviceAccountJson
            )
        )

        StorageBucket.Kind.S3 -> S3BucketClient(
            S3BucketClient.Settings(
                bucket = bucket.bucketName,
                region = bucket.region ?: error("An S3 bucket needs a region"),
                endpoint = bucket.endpoint,
                publicBaseUrl = bucket.publicBaseUrl,
                accessKeyId = bucket.accessKeyId,
                secretAccessKey = bucket.secretAccessKey
            )
        )
    }
}

/**
 * The buckets listed on the Storage page, decrypted, each with its addresses and a client built the
 * first time it is needed.
 *
 * Read once and held for [CACHE_TTL], since every upload and every opened document asks. A change made
 * on this instance is seen at once ([forget]); one made on another is seen here within the TTL, which
 * is why an admin waits that long after sending new files elsewhere before copying the old bucket.
 */
@Service
class StorageBuckets(
    private val repository: StorageBucketRepository,
    private val crypto: CryptoService,
    private val clients: BucketClientFactory
) {

    class Entry(val row: StorageBucket, val addresses: BucketAddresses, client: () -> BucketClient) {
        val client: BucketClient by lazy(client)
    }

    /** [address] is [name] in [holder]'s bucket, and its bytes are in [reader]'s. */
    data class Located(val holder: Entry, val name: String, val reader: Entry)

    private class Snapshot(val entries: List<Entry>, val loadedAt: Instant)

    private val snapshot = AtomicReference<Snapshot?>()

    /** The connection a client was built for, so one is rebuilt only when that changes. */
    private val built = ConcurrentHashMap<ObjectId, Pair<StorageBucket, BucketClient>>()

    fun all(): List<Entry> {
        snapshot.get()?.takeIf { Duration.between(it.loadedAt, Instant.now()) < CACHE_TTL }?.let { return it.entries }
        val entries = repository.findAll().sortedBy { it.createdAt }.map { encrypted ->
            val row = encrypted.copy(
                secretAccessKey = encrypted.secretAccessKey?.let(crypto::decrypt),
                serviceAccountJson = encrypted.serviceAccountJson?.let(crypto::decrypt)
            )
            Entry(row, row.addresses()) { clientFor(row) }
        }
        built.keys.retainAll(entries.map { it.row.id }.toSet())
        snapshot.set(Snapshot(entries, Instant.now()))
        return entries
    }

    fun byId(id: ObjectId): Entry? = all().firstOrNull { it.row.id == id }

    fun primary(): Entry? = all().firstOrNull { it.row.primary }

    fun locate(address: String): Located? {
        val entries = all()
        val (holder, name) = entries.firstNotNullOfOrNull { entry ->
            entry.addresses.nameOf(address)?.let { entry to it }
        } ?: return null
        return Located(holder, name, readerOf(holder, entries))
    }

    /** Where [entry]'s files are read from, following one copy after another. */
    fun readerOf(entry: Entry, entries: List<Entry> = all()): Entry {
        var reader = entry
        val seen = mutableSetOf(entry.row.id)
        while (true) {
            val next = reader.row.readFromBucketId ?: return reader
            reader = entries.firstOrNull { it.row.id == next }
                ?: error("${reader.row.name} reads its files from a bucket that is no longer listed")
            check(seen.add(next)) { "${entry.row.name} reads its files from a bucket that reads them back from it" }
        }
    }

    /** Drops what this instance holds, after it changed the list itself. */
    fun forget() {
        snapshot.set(null)
    }

    private fun clientFor(row: StorageBucket): BucketClient {
        val connection = row.copy(name = "", primary = false, readFromBucketId = null, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
        return built.compute(row.id) { _, existing ->
            if (existing != null && existing.first == connection) existing else connection to clients.build(row)
        }!!.second
    }

    companion object {
        val CACHE_TTL: Duration = Duration.ofSeconds(30)
    }
}
