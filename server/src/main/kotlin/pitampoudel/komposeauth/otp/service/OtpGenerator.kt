package pitampoudel.komposeauth.otp.service

import java.security.SecureRandom

/**
 * The six digits that get texted or emailed to somebody as proof they own an address.
 *
 * These are a credential, not a nonce. `Credential.OTP` signs a user in on its own — creating the
 * account if it does not exist yet — so for the five minutes a code lives it is worth what a
 * password is worth. Both call sites used to reach for `kotlin.random.Random`, which is a `XorWow`
 * generator seeded once from the clock: its state is reconstructible from a short run of outputs,
 * and every code it will ever emit follows from that state. Requesting codes to an address you
 * already own is free, so collecting that run costs an attacker nothing — and then the code the
 * next person is sent can be read ahead of them.
 *
 * [SecureRandom] holds no such relationship between its outputs. One instance is shared because it
 * is thread-safe and reseeds itself; a fresh one per code would only pay for seeding again.
 */
object OtpGenerator {

    /**
     * Every code a caller may be sent, ends included.
     *
     * Six digits with no leading zero, so nothing downstream has to decide whether `012345` and
     * `12345` are the same code. Note where it ends: the `Random.nextInt(100000, 999999)` this
     * replaces took an *exclusive* upper bound, so `999999` was never a code anyone could be sent.
     */
    val RANGE: IntRange = 100_000..999_999

    private val secureRandom = SecureRandom()

    /**
     * A uniform draw from [RANGE].
     *
     * `nextInt(bound)` rejects and redraws rather than folding the excess with a modulo, so no code
     * comes up more often than another.
     */
    fun next(): String =
        (RANGE.first + secureRandom.nextInt(RANGE.last - RANGE.first + 1)).toString()
}
