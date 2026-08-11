package pitampoudel.komposeauth.core.security.ratelimit

import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Where the counters live.
 *
 * Split out from [RateLimiter] so the window arithmetic can be exercised without a database, and so
 * the backing store can change without touching the call sites.
 */
interface RateLimitStore {
    /**
     * Adds one to the counter for [bucketKey] and returns its new total.
     *
     * Must be atomic: two instances counting the same bucket at the same moment have to produce two
     * distinct totals, or the limit is only advisory. [expiresAt] applies to the bucket as a whole
     * and is set when the bucket is first created, never extended.
     */
    fun incrementAndCount(bucketKey: String, expiresAt: Instant): Long
}

/**
 * Process-local counters, for tests.
 *
 * Not wired into the running application, which uses [MongoRateLimitStore]: counters kept in one
 * process are correct only where a single instance sees every request, and on more than one instance
 * — or on serverless, where instances come and go constantly — each holds its own tally, so the
 * effective limit multiplies by the instance count and resets on every cold start.
 */
class InMemoryRateLimitStore(
    /** Shares the caller's clock so pruning cannot disagree with it about what has expired. */
    private val clock: Clock = Clock.systemUTC()
) : RateLimitStore {

    private data class Bucket(val expiresAt: Instant, val count: AtomicLong)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun incrementAndCount(bucketKey: String, expiresAt: Instant): Long {
        // The key already names the window, so a new window is simply a new entry — same as the
        // Mongo store, where the document `_id` distinguishes windows. `expiresAt` is only ever a
        // hint for pruning; deciding expiry here would disagree with the caller about which window
        // is current.
        if (buckets.size >= CLEANUP_THRESHOLD) {
            val now = clock.instant()
            buckets.entries.removeIf { it.value.expiresAt.isBefore(now) }
        }
        return buckets
            .computeIfAbsent(bucketKey) { Bucket(expiresAt, AtomicLong(0)) }
            .count
            .incrementAndGet()
    }

    private companion object {
        const val CLEANUP_THRESHOLD = 10_000
    }
}
