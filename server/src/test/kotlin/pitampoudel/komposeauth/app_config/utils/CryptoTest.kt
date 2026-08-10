package pitampoudel.komposeauth.app_config.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CryptoTest {

    private val key = Crypto.keyFromString("a-sufficiently-long-test-key")

    @Test
    fun `round-trips a value`() {
        val encrypted = Crypto.encrypt("hunter2", key)
        assertEquals("hunter2", Crypto.decrypt(encrypted, key))
    }

    @Test
    fun `derives a full-length AES key`() {
        assertEquals(32, key.encoded.size)
    }

    @Test
    fun `rejects a key too short to carry real entropy`() {
        // Regression: short keys used to be right-padded with zero bytes, producing a 256-bit key
        // with almost no entropy behind it and no indication anything was wrong.
        assertThrows<IllegalArgumentException> { Crypto.keyFromString("short") }
    }

    @Test
    fun `uses a fresh iv per encryption`() {
        val first = Crypto.encrypt("same plaintext", key)
        val second = Crypto.encrypt("same plaintext", key)

        assertNotEquals(first.iv, second.iv)
        assertNotEquals(first.cipherText, second.cipherText)
    }
}
