package pitampoudel.komposeauth.user.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import pitampoudel.komposeauth.core.security.ratelimit.InMemoryRateLimitStore
import pitampoudel.komposeauth.core.security.ratelimit.RateLimitProperties
import pitampoudel.komposeauth.core.security.ratelimit.RateLimiter
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.utils.ServerUrl
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Minting the token and talking to the mail server take time only an address with an account
 * spends, so they happen after the answer, not before it.
 */
class PasswordResetMailTest {

    private val userService = mockk<UserService>()
    private val emailService = mockk<EmailService>()
    private val oneTimeTokenService = mockk<OneTimeTokenService>()
    private val scheduled = mutableListOf<Runnable>()

    private val controller = PasswordResetController(
        userService = userService,
        emailService = emailService,
        oneTimeTokenService = oneTimeTokenService,
        appConfigService = mockk(),
        serverUrl = ServerUrl(AuthorizationServerSettings.builder().issuer("https://auth.example.com").build()),
        taskExecutor = TaskExecutor { scheduled += it },
        rateLimiter = RateLimiter(InMemoryRateLimitStore(), Clock.systemUTC()),
        rateLimitProperties = RateLimitProperties()
    )

    private val user = User(
        id = ObjectId.get(),
        firstName = "Owner",
        lastName = "One",
        email = "owner@example.com",
        phoneNumber = null
    )

    @Test
    fun `the reset mail is sent after the answer`() {
        every { userService.findByUserName("owner@example.com") } returns user
        every { oneTimeTokenService.generateResetPasswordLink(user.id, any(), "https://auth.example.com") } returns
            "https://auth.example.com/reset-password?token=t"
        every { emailService.sendHtmlMail(any(), any(), any(), any(), any()) } returns true

        assertEquals(200, controller.sendResetLink("owner@example.com").statusCode.value())
        verify(exactly = 0) { oneTimeTokenService.generateResetPasswordLink(any(), any(), any()) }
        verify(exactly = 0) { emailService.sendHtmlMail(any(), any(), any(), any(), any()) }

        scheduled.single().run()
        verify(exactly = 1) { emailService.sendHtmlMail("https://auth.example.com", "owner@example.com", any(), any(), any()) }
    }

    @Test
    fun `a failure after the answer is caught rather than lost on the worker`() {
        every { userService.findByUserName("owner@example.com") } returns user
        every { oneTimeTokenService.generateResetPasswordLink(any(), any(), any()) } throws IllegalStateException("db down")

        controller.sendResetLink("owner@example.com")
        scheduled.single().run()
    }

    @Test
    fun `one inbox gets only so many reset mails, and the answer never says so`() {
        every { userService.findByUserName(any()) } returns user
        every { oneTimeTokenService.generateResetPasswordLink(any(), any(), any()) } returns
            "https://auth.example.com/reset-password?token=t"
        every { emailService.sendHtmlMail(any(), any(), any(), any(), any()) } returns true
        val limit = RateLimitProperties().passwordResetPerRecipient.limit

        // Asked for by the address or by the account's phone number alike: it is the inbox that is counted.
        repeat(limit + 2) { i ->
            val response = controller.sendResetLink(if (i % 2 == 0) "owner@example.com" else "+9779812345678")
            assertEquals(200, response.statusCode.value())
        }
        scheduled.forEach { it.run() }

        verify(exactly = limit) { emailService.sendHtmlMail(any(), "owner@example.com", any(), any(), any()) }
        verify(exactly = limit) { oneTimeTokenService.generateResetPasswordLink(any(), any(), any()) }
    }

    @Test
    fun `an address with no account schedules nothing`() {
        every { userService.findByUserName(any()) } returns null

        assertEquals(200, controller.sendResetLink("nobody@example.com").statusCode.value())
        assertTrue(scheduled.isEmpty())
    }
}
