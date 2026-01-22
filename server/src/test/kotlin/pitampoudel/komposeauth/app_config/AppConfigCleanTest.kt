package pitampoudel.komposeauth.app_config

import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.app_config.entity.AppConfig
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppConfigCleanTest {

    @Test
    fun `clean normalizes blank strings to null`() {
        val config = AppConfig(
            name = "  ",
            logoUrl = "",
            websiteUrl = "https://example.com",
            googleAuthClientSecret = "   ",
            smtpHost = "smtp.example.com",
            smtpUsername = "\n\t ",
            smtpPort = 587,
        )

        val cleaned = config.clean()

        assertNull(cleaned.name)
        assertNull(cleaned.logoUrl)
        assertEquals("https://example.com", cleaned.websiteUrl)
        assertNull(cleaned.googleAuthClientSecret)
        assertEquals("smtp.example.com", cleaned.smtpHost)
        assertNull(cleaned.smtpUsername)
        assertEquals(587, cleaned.smtpPort)
    }

    @Test
    fun `clean normalizes blank smsProvider to null`() {
        val config = AppConfig(
            smsProvider = "  ",
            twilioAccountSid = "AC123",
            samayeApiKey = "key123"
        )

        val cleaned = config.clean()

        assertNull(cleaned.smsProvider)
        assertEquals("AC123", cleaned.twilioAccountSid)
        assertEquals("key123", cleaned.samayeApiKey)
    }

    @Test
    fun `clean preserves valid smsProvider values`() {
        val twilioConfig = AppConfig(smsProvider = "twilio")
        assertEquals("twilio", twilioConfig.clean().smsProvider)

        val samayeConfig = AppConfig(smsProvider = "samaye")
        assertEquals("samaye", samayeConfig.clean().smsProvider)

        val emptyConfig = AppConfig(smsProvider = "")
        assertNull(emptyConfig.clean().smsProvider)
    }
}

