package pitampoudel.komposeauth.core.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class EmailServiceTest {

    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    private lateinit var appConfigProvider: AppConfigProvider

    /**
     * A mail host that accepts the connection and then says nothing -- which is what a filtered
     * SMTP port or a provider rate-limiting by going quiet looks like from here, and the case
     * JavaMail waits out forever unless it is told not to.
     *
     * This is the regression: with no `mail.smtp.timeout` the send never returns, the request
     * thread is pinned permanently rather than for the length of the request, and the visitor
     * watches a page spin until a proxy somewhere gives up. Nothing is thrown, so the `catch` in
     * `sendHtmlMail` never runs and nothing is logged either.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `a mail host that accepts and then goes silent fails instead of hanging`() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { silentHost ->
            // Accept, then hold the connection open without sending the greeting an SMTP client waits for.
            val accepting = thread(isDaemon = true) {
                runCatching { while (true) silentHost.accept() }
            }
            val previous = appConfigProvider.get()
            try {
                appConfigProvider.save(
                    AppConfig(
                        smtpHost = silentHost.inetAddress.hostAddress,
                        smtpPort = silentHost.localPort,
                        smtpFromEmail = "no-reply@example.com"
                    )
                )

                val result = emailService.sendHtmlMail(
                    baseUrl = "http://localhost:8080",
                    to = "someone@example.com",
                    subject = "Timeout Test",
                    template = "email/generic",
                    model = mapOf("recipientName" to "Test")
                )

                assertFalse(result, "a silent mail host should fail the send, not succeed")
            } finally {
                // Shared context: put the configuration back for whatever runs next.
                appConfigProvider.save(previous.copy())
                accepting.interrupt()
            }
        }
    }

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
