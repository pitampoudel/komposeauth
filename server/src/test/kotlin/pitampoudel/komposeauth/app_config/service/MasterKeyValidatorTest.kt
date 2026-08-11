package pitampoudel.komposeauth.app_config.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.StaticAppProperties

class MasterKeyValidatorTest {

    private val key = "Zm9vYmFyYmF6cXV4Zm9vYmFyYmF6cXV4Zm9vYmFyYmE="

    private val validator = MasterKeyValidator(
        StaticAppProperties().apply { base64EncryptionKey = key }
    )

    @Test
    fun `accepts the configured key`() {
        assertTrue(validator.isValid(key))
    }

    @Test
    fun `accepts a key whose plus signs were decoded as spaces`() {
        // Base64 contains `+`, which some form and query decodings turn into a space on the way in.
        val validatorForPlusKey = MasterKeyValidator(
            StaticAppProperties().apply { base64EncryptionKey = "abc+def/ghi=" }
        )

        assertTrue(validatorForPlusKey.isValid("abc def/ghi="))
    }

    @Test
    fun `rejects a wrong key of the same length`() {
        val wrong = key.dropLast(1) + if (key.last() == 'A') 'B' else 'A'

        assertFalse(validator.isValid(wrong))
    }

    @Test
    fun `rejects a prefix of the real key`() {
        // Compared as digests, so a candidate of a different length takes the same path as any other
        // wrong value rather than failing early and revealing the real key's length.
        assertFalse(validator.isValid(key.take(8)))
    }

    @Test
    fun `rejects a key with the right prefix but extra trailing characters`() {
        assertFalse(validator.isValid(key + "extra"))
    }

    @Test
    fun `rejects nothing at all`() {
        assertFalse(validator.isValid(null))
        assertFalse(validator.isValid(""))
    }
}
