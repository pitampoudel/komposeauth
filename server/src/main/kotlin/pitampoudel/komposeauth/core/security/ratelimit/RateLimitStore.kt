package pitampoudel.komposeauth.core.security.ratelimit

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
 * Process-local counters.
 *
 * Correct only where a single instance sees every request. On more than one instance — and on
 * serverless, where instances come and go constantly — each holds its own tally, so the effective
 * limit multiplies by the instance count and resets on every cold start. Used for tests and as the
 * fallback when no database-backed store is available.
 */
class InMemoryRateLimitStore : RateLimitStore {

    private data class Bucket(val expiresAt: Instant, val count: AtomicLong)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun incrementAndCount(bucketKey: String, expiresAt: Instant): Long {
        val now = Instant.now()
        if (buckets.size >= CLEANUP_THRESHOLD) {
            buckets.entries.removeIf { it.value.expiresAt.isBefore(now) }
        }
        val bucket = buckets.compute(bucketKey) { _, existing ->
            if (existing == null || existing.expiresAt.isBefore(now)) {
                Bucket(expiresAt, AtomicLong(0))
            } else {
                existing
            }
        }!!
        return bucket.count.incrementAndGet()
    }

    private companion object {
        const val CLEANUP_THRESHOLD = 10_000
    }
}
