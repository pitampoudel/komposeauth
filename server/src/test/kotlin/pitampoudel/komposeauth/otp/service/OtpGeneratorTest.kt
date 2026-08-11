package pitampoudel.komposeauth.otp.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OtpGeneratorTest {

    /**
     * The shape callers and templates assume: six digits, no leading zero, nothing else.
     */
    @Test
    fun `every code is six digits`() {
        val malformed = (1..DRAWS)
            .map { OtpGenerator.next() }
            .filterNot { it.matches(Regex("^[1-9][0-9]{5}$")) }
            .distinct()

        assertTrue(malformed.isEmpty(), "not six digits: $malformed")
    }

    @Test
    fun `every code lies in the advertised range`() {
        val outside = (1..DRAWS)
            .map { OtpGenerator.next().toInt() }
            .filterNot { it in OtpGenerator.RANGE }

        assertTrue(outside.isEmpty(), "outside ${OtpGenerator.RANGE}: ${outside.take(5)}")
    }

    /**
     * Pins the ends rather than sampling for them. `999999` is reachable only once per ~900k draws,
     * so a test that waited to observe it would be a test that usually proves nothing — but it was
     * unreachable *at all* under the exclusive-bound `Random.nextInt(100000, 999999)` this replaced,
     * and that is worth stating somewhere it cannot silently come back.
     */
    @Test
    fun `the range covers every six-digit code`() {
        assertEquals(100_000..999_999, OtpGenerator.RANGE)
    }

    /**
     * A generator that had got stuck — a constant, a short cycle, a counter reset per call — would
     * still satisfy the format checks above.
     *
     * The bar has to be set off the birthday count, not off intuition: 20k draws from 900k values
     * collide about n²/2N ≈ 222 times, with a standard deviation near 15. So repeats are *expected*,
     * a shade over 1% of the sample, and a "codes are 99% distinct" assertion would fail as often as
     * it passed. [MAX_REPEATS] sits some fifty deviations above that — far enough that a healthy
     * generator will not trip it, and nowhere near the many thousands of repeats a degenerate one
     * would produce.
     */
    @Test
    fun `codes do not repeat`() {
        val codes = (1..DRAWS).map { OtpGenerator.next() }
        val repeats = DRAWS - codes.distinct().size

        assertTrue(
            repeats <= MAX_REPEATS,
            "$repeats repeated codes in $DRAWS draws, expected around 222"
        )
    }

    private companion object {
        const val DRAWS = 20_000
        const val MAX_REPEATS = 1_000
    }
}
