package pitampoudel.komposeauth.core.security.ratelimit

import java.time.Instant

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
