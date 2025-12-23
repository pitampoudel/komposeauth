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
}

