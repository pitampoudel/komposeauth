package pitampoudel.komposeauth.core.security.ratelimit

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fixed-window request counter used to blunt credential stuffing and, on the OTP endpoints, to stop
 * an attacker from running up an SMS bill against someone else's phone number.
 *
 * State is held in this process. Behind more than one instance the effective limit multiplies by the
 * instance count, which still bounds abuse but is not exact — move the counters to a shared store
 * (Redis, or the existing Mongo) if precise global limits are needed.
 */
@Service
class RateLimiter {

    private data class Window(val resetAt: Instant, val count: AtomicInteger)

    private val windows = ConcurrentHashMap<String, Window>()

    /** Cheap amortised cleanup so abandoned keys don't accumulate. */
    private fun sweepIfNeeded(now: Instant) {
        if (windows.size < CLEANUP_THRESHOLD) return
        windows.entries.removeIf { it.value.resetAt.isBefore(now) }
    }

    /**
     * Records a hit against [key]. Returns false once more than [limit] hits land inside [window].
     */
    fun tryAcquire(key: String, limit: Int, window: Duration): Boolean {
        val now = Instant.now()
        sweepIfNeeded(now)
        val current = windows.compute(key) { _, existing ->
            if (existing == null || existing.resetAt.isBefore(now)) {
                Window(now.plus(window), AtomicInteger(0))
            } else {
                existing
            }
        }!!
        return current.count.incrementAndGet() <= limit
    }

    /** As [tryAcquire], but raises 429 with a Retry-After hint instead of returning false. */
    fun enforce(key: String, limit: Int, window: Duration, message: String) {
        if (!tryAcquire(key, limit, window)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message)
        }
    }

    /** Seconds until [key]'s window resets, for a Retry-After header. */
    fun retryAfterSeconds(key: String): Long {
        val resetAt = windows[key]?.resetAt ?: return 0
        return maxOf(0, Duration.between(Instant.now(), resetAt).seconds)
    }

    private companion object {
        const val CLEANUP_THRESHOLD = 10_000
    }
}
