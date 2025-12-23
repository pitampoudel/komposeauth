package pitampoudel.komposeauth.app_config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.StaticAppProperties
import pitampoudel.komposeauth.app_config.service.MasterKeyValidator

class MasterKeyValidatorTest {

    @Test
    fun `isValid returns false for null and blank`() {
        val props = StaticAppProperties().apply { base64EncryptionKey = "abc+123" }
        val validator = MasterKeyValidator(props)

        assertFalse(validator.isValid(null))
        assertFalse(validator.isValid(""))
        assertFalse(validator.isValid("   "))
    }

    @Test
    fun `isValid treats space as plus`() {
        val props = StaticAppProperties().apply { base64EncryptionKey = "abc+123" }
        val validator = MasterKeyValidator(props)

        assertTrue(validator.isValid("abc 123"))
        assertTrue(validator.isValid("abc+123"))
        assertFalse(validator.isValid("abc%2B123"))
    }
}
