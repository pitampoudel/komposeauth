package pitampoudel.komposeauth.core.service.sms

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.otp.service.NoOpPhoneNumberVerificationService


class NoOpPhoneNumberVerificationServiceTest {

    @Test
    fun `initiate always returns false`() {
        val service = NoOpPhoneNumberVerificationService()

        val result = service.initiate("+1234567890")

        assertFalse(result, "NoOpPhoneNumberVerificationService.initiate should always return false")
    }

    @Test
    fun `verify always returns false`() {
        val service = NoOpPhoneNumberVerificationService()

        val result = service.verify("+1234567890", "123456")

        assertFalse(result, "NoOpPhoneNumberVerificationService.verify should always return false")
    }

    @Test
    fun `initiate handles various phone formats`() {
        val service = NoOpPhoneNumberVerificationService()

        val phoneNumbers = listOf(
            "+1234567890",
            "+44 7700 900000",
            "+977-9841234567",
            "1234567890"
        )

        phoneNumbers.forEach { phone ->
            val result = service.initiate(phone)
            assertFalse(result)
        }
    }

    @Test
    fun `verify handles various code formats`() {
        val service = NoOpPhoneNumberVerificationService()

        val codes = listOf("123456", "000000", "999999", "ABC123")

        codes.forEach { code ->
            val result = service.verify("+1234567890", code)
            assertFalse(result)
        }
    }

    @Test
    fun `verify handles empty code`() {
        val service = NoOpPhoneNumberVerificationService()

        val result = service.verify("+1234567890", "")

        assertFalse(result)
    }

    @Test
    fun `initiate and verify are independent`() {
        val service = NoOpPhoneNumberVerificationService()

        // Initiate doesn't affect verify
        service.initiate("+1234567890")
        val result = service.verify("+1234567890", "123456")

        assertFalse(result)
    }
}
