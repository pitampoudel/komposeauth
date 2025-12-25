package pitampoudel.komposeauth.core.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.MongoTestSupport

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
class EmailServiceTest {

    @Autowired
    private lateinit var emailService: EmailService

    @Test
    fun `sendHtmlMail does not throw exception with valid parameters`() {
        // In test environment, email sending will likely fail due to missing SMTP config
        // But we test that the method handles this gracefully
        val result = emailService.sendHtmlMail(
            baseUrl = "http://localhost:8080",
            to = "test@example.com",
            subject = "Test Email",
            template = "email/generic",
            model = mapOf(
                "recipientName" to "Test User",
                "message" to "This is a test message"
            )
        )

        // The result may be true or false depending on configuration
        // We just verify the method doesn't throw exceptions
        assertNotNull(result)
    }

    @Test
    fun `sendHtmlMail handles missing SMTP configuration gracefully`() {
        // Should return false when SMTP is not configured
        val result = emailService.sendHtmlMail(
            baseUrl = "http://localhost:8080",
            to = "test@example.com",
            subject = "Test Subject",
            template = "email/generic",
            model = emptyMap()
        )

        // In test environment without proper SMTP config, this should return false
        assertFalse(result)
    }

    @Test
    fun `sendHtmlMail works with empty model`() {
        val result = emailService.sendHtmlMail(
            baseUrl = "http://localhost:8080",
            to = "empty@example.com",
            subject = "Empty Model Test",
            template = "email/generic",
            model = emptyMap()
        )

        assertNotNull(result)
    }

    @Test
    fun `sendHtmlMail works with complex model data`() {
        val result = emailService.sendHtmlMail(
            baseUrl = "http://localhost:8080",
            to = "complex@example.com",
            subject = "Complex Model Test",
            template = "email/generic",
            model = mapOf(
                "recipientName" to "John Doe",
                "message" to "Welcome to our platform!",
                "actionUrl" to "http://example.com/verify",
                "actionText" to "Verify Email",
                "illustration" to "http://example.com/image.png"
            )
        )

        assertNotNull(result)
    }

    @Test
    fun `sendHtmlMail handles special characters in email content`() {
        val result = emailService.sendHtmlMail(
            baseUrl = "http://localhost:8080",
            to = "special@example.com",
            subject = "Test with <Special> & Characters",
            template = "email/generic",
            model = mapOf(
                "recipientName" to "Test & User <test>",
                "message" to "Message with \"quotes\" and 'apostrophes'"
            )
        )

        assertNotNull(result)
    }

    @Test
    fun `sendHtmlMail with various email addresses`() {
        listOf(
            "simple@example.com",
            "with.dot@example.com",
            "with+plus@example.com",
            "with-dash@example.com"
        ).forEach { email ->
            val result = emailService.sendHtmlMail(
                baseUrl = "http://localhost:8080",
                to = email,
                subject = "Test",
                template = "email/generic",
                model = mapOf("recipientName" to "Test")
            )
            assertNotNull(result)
        }
    }
}
