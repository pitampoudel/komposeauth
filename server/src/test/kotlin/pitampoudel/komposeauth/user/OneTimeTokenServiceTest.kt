package pitampoudel.komposeauth.user

import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.hours

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(OneTimeTokenService::class, TestConfig::class)
class OneTimeTokenServiceTest {

    @Autowired
    private lateinit var service: OneTimeTokenService

    private companion object {
        const val CONTENDERS = 8

        /**
         * A race can be lost and still come out right by luck, so this is run more than once.
         * Against the read-then-write it replaces, eight contenders all spent the same token on the
         * first round — the repeats are headroom for a future regression that only sometimes races,
         * not for catching this one.
         */
        const val ATTEMPTS = 5
    }


    @Test
    fun `createToken stores hashed token, findValidToken succeeds, consume makes it single-use`() {
        val userId = ObjectId()
        val raw = service.createToken(userId, OneTimeToken.Purpose.RESET_PASSWORD, ttl = 1.hours)

        // Repository should never store raw token
        val stored = service.findValidToken(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        assertNotNull(stored.tokenHash)

        val validated = service.findValidToken(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        assertEquals(stored.id, validated.id)

        val consumed = service.consume(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        assertNotNull(consumed.consumedAt)

        // A spent token is a client error, not a server fault: it now surfaces as 400 rather
        // than escaping as an unhandled 500.
        val spent = assertThrows(ResponseStatusException::class.java) {
            service.findValidToken(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        }
        assertEquals(HttpStatus.BAD_REQUEST, spent.statusCode)
    }

    /**
     * Two holders of the same link, arriving together.
     *
     * The point of a one-time token is that exactly one of them gets to spend it, and the read-then-
     * write this replaced could not promise that: both reads saw an unspent token, so both callers
     * were told to go ahead. For `REFRESH_TOKEN` that is a leaked token redeemed as many times as it
     * is presented at once; for `RESET_PASSWORD` it is two submissions both setting the password.
     */
    @Test
    fun `only one of two concurrent callers can consume a token`() {
        repeat(ATTEMPTS) {
            val raw = service.createToken(ObjectId(), OneTimeToken.Purpose.REFRESH_TOKEN, ttl = 1.hours)

            val pool = Executors.newFixedThreadPool(CONTENDERS)
            // Everyone waits on the latch, so the calls overlap instead of queuing behind thread
            // startup — without it the first would routinely finish before the second began.
            val start = CountDownLatch(1)
            val succeeded = AtomicInteger()
            try {
                val racers = (1..CONTENDERS).map {
                    pool.submit {
                        start.await()
                        try {
                            service.consume(raw, OneTimeToken.Purpose.REFRESH_TOKEN)
                            succeeded.incrementAndGet()
                        } catch (_: ResponseStatusException) {
                            // The losers, which is the whole point.
                        }
                    }
                }
                start.countDown()
                racers.forEach { it.get(30, TimeUnit.SECONDS) }
            } finally {
                pool.shutdownNow()
            }

            assertEquals(1, succeeded.get(), "token was spent ${succeeded.get()} times, not once")
        }
    }

    @Test
    fun `findValidToken rejects purpose mismatch`() {
        val userId = ObjectId()
        val raw = service.createToken(userId, OneTimeToken.Purpose.VERIFY_EMAIL, ttl = 1.hours)

        val mismatch = assertThrows(ResponseStatusException::class.java) {
            service.findValidToken(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        }
        assertEquals(HttpStatus.BAD_REQUEST, mismatch.statusCode)
        // Same wording as an unknown token, so the response can't distinguish the two.
        assertEquals("This link is invalid or has expired. Request a new one.", mismatch.reason)
    }

}
