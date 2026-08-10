package pitampoudel.komposeauth.core.security.ratelimit

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Fixed-window request counter used to blunt credential stuffing and, on the OTP endpoints, to stop
 * an attacker running up an SMS bill against someone else's phone number.
 *
 * Windows are aligned to the clock rather than to first use, so every instance derives the same
 * window boundary for the same key without coordinating. Counting itself is delegated to a
 * [RateLimitStore]; in production that is Mongo, which is what keeps the limits meaningful across
 * cold starts and multiple instances.
 */
@Service
class RateLimiter(
    private val store: RateLimitStore,
    /** Injected so window rollover can be tested without sleeping through real seconds. */
    private val clock: Clock
) {

    /** @param retryAfterSeconds how long until the current window rolls over. */
    data class Decision(val allowed: Boolean, val retryAfterSeconds: Long)

    /**
     * Records a hit against [key] and reports whether it fits inside [limit] for this [window].
     */
    fun check(key: String, limit: Int, window: Duration): Decision {
        val windowSeconds = window.seconds.coerceAtLeast(1)
        val nowSeconds = clock.instant().epochSecond
        val windowIndex = Math.floorDiv(nowSeconds, windowSeconds)
        val windowEndSeconds = (windowIndex + 1) * windowSeconds

        val count = store.incrementAndCount(
            bucketKey = "$key|$windowIndex",
            // A little past the window so the TTL monitor can't drop a bucket that is still counting.
            expiresAt = Instant.ofEpochSecond(windowEndSeconds).plusSeconds(EXPIRY_GRACE_SECONDS)
        )

        return Decision(
            allowed = count <= limit,
            retryAfterSeconds = (windowEndSeconds - nowSeconds).coerceAtLeast(1)
        )
    }

    /** Records a hit and returns whether it was within budget. */
    fun tryAcquire(key: String, limit: Int, window: Duration): Boolean =
        check(key, limit, window).allowed

    /** As [tryAcquire], but raises 429 with a Retry-After hint instead of returning false. */
    fun enforce(key: String, limit: Int, window: Duration, message: String) {
        val decision = check(key, limit, window)
        if (!decision.allowed) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "$message Try again in ${decision.retryAfterSeconds} seconds."
            )
        }
    }

    private companion object {
        const val EXPIRY_GRACE_SECONDS = 60L
    }
}
