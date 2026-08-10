package pitampoudel.komposeauth.core.security.ratelimit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

class RateLimiterTest {

    @Test
    fun `allows up to the limit then refuses`() {
        val limiter = RateLimiter()
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
        val limiter = RateLimiter()
        val window = Duration.ofMinutes(5)

        assertTrue(limiter.tryAcquire("a", limit = 1, window = window))
        assertFalse(limiter.tryAcquire("a", limit = 1, window = window))
        // A different caller must not inherit the first one's exhausted budget.
        assertTrue(limiter.tryAcquire("b", limit = 1, window = window))
    }

    @Test
    fun `starts a fresh window once the old one has elapsed`() {
        val limiter = RateLimiter()
        val expired = Duration.ofMillis(1)

        assertTrue(limiter.tryAcquire("k", limit = 1, window = expired))
        assertFalse(limiter.tryAcquire("k", limit = 1, window = expired))

        Thread.sleep(5)

        assertTrue(limiter.tryAcquire("k", limit = 1, window = expired))
    }

    @Test
    fun `enforce raises 429 once the budget is spent`() {
        val limiter = RateLimiter()
        val window = Duration.ofMinutes(5)

        limiter.enforce("k", limit = 1, window = window, message = "slow down")

        val ex = assertThrows<ResponseStatusException> {
            limiter.enforce("k", limit = 1, window = window, message = "slow down")
        }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.statusCode)
    }
}
