package pitampoudel.komposeauth.core.security.ratelimit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class RateLimiterTest {

    private fun limiter() = RateLimiter(InMemoryRateLimitStore())

    @Test
    fun `allows up to the limit then refuses`() {
        val limiter = limiter()
        val window = Duration.ofMinutes(5)

        repeat(3) { attempt ->
            assertTrue(
                limiter.tryAcquire("k", limit = 3, window = window),
                "attempt ${attempt + 1} should be allowed"
            )
        }
        assertFalse(limiter.tryAcquire("k", limit = 3, window = window))
        assertFalse(limiter.tryAcquire("k", limit = 3, window = window))
    }

    @Test
    fun `counts each key separately`() {
        val limiter = limiter()
        val window = Duration.ofMinutes(5)

        assertTrue(limiter.tryAcquire("a", limit = 1, window = window))
        assertFalse(limiter.tryAcquire("a", limit = 1, window = window))
        // A different caller must not inherit the first one's exhausted budget.
        assertTrue(limiter.tryAcquire("b", limit = 1, window = window))
    }

    @Test
    fun `starts a fresh window once the old one has elapsed`() {
        val limiter = limiter()
        val oneSecond = Duration.ofSeconds(1)

        // Windows are clock-aligned, so land inside one before spending it.
        while (Instant.now().nano > 300_000_000) Thread.sleep(10)

        assertTrue(limiter.tryAcquire("k", limit = 1, window = oneSecond))
        assertFalse(limiter.tryAcquire("k", limit = 1, window = oneSecond))

        Thread.sleep(900)

        assertTrue(limiter.tryAcquire("k", limit = 1, window = oneSecond))
    }

    @Test
    fun `enforce raises 429 once the budget is spent`() {
        val limiter = limiter()
        val window = Duration.ofMinutes(5)

        limiter.enforce("k", limit = 1, window = window, message = "slow down")

        val ex = assertThrows<ResponseStatusException> {
            limiter.enforce("k", limit = 1, window = window, message = "slow down")
        }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.statusCode)
        assertTrue(ex.reason!!.contains("Try again in"), "should tell the caller when to retry")
    }

    @Test
    fun `reports a retry hint inside the window`() {
        val decision = limiter().check("k", limit = 1, window = Duration.ofMinutes(5))

        assertTrue(decision.allowed)
        assertTrue(
            decision.retryAfterSeconds in 1..300,
            "retry hint should fall inside the window, was ${decision.retryAfterSeconds}"
        )
    }

    @Test
    fun `instances sharing a store share one budget`() {
        // What the Mongo-backed store buys on serverless: the tally follows the key, not the
        // process, so scaling out or cold-starting doesn't hand an attacker a fresh allowance.
        val shared = InMemoryRateLimitStore()
        val instanceA = RateLimiter(shared)
        val instanceB = RateLimiter(shared)
        val window = Duration.ofMinutes(5)

        assertTrue(instanceA.tryAcquire("k", limit = 2, window = window))
        assertTrue(instanceB.tryAcquire("k", limit = 2, window = window))
        assertFalse(instanceB.tryAcquire("k", limit = 2, window = window))
        assertFalse(instanceA.tryAcquire("k", limit = 2, window = window))
    }

    @Test
    fun `allows the request when the counter is unavailable`() {
        // Fail open: a store outage must not take sign-in down with it.
        val brokenStore = object : RateLimitStore {
            override fun incrementAndCount(bucketKey: String, expiresAt: Instant): Long = 0
        }

        assertTrue(RateLimiter(brokenStore).tryAcquire("k", limit = 1, window = Duration.ofMinutes(5)))
    }

    @Test
    fun `keys each window separately so a bucket cannot be reused`() {
        val seen = ConcurrentHashMap<String, AtomicLong>()
        val recordingStore = object : RateLimitStore {
            override fun incrementAndCount(bucketKey: String, expiresAt: Instant): Long =
                seen.computeIfAbsent(bucketKey) { AtomicLong() }.incrementAndGet()
        }
        val limiter = RateLimiter(recordingStore)

        limiter.tryAcquire("k", limit = 5, window = Duration.ofMinutes(1))
        limiter.tryAcquire("k", limit = 5, window = Duration.ofMinutes(5))

        // Same key, different window lengths must not collide onto one counter.
        assertEquals(2, seen.size, "expected distinct buckets, got ${seen.keys}")
        assertTrue(seen.keys.all { it.startsWith("k|") })
    }
}
