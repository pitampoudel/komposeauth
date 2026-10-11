package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.user.entity.User
import kotlin.test.assertTrue

class ContactChangeNotifierTest {
    private val emailService = mock<EmailService>()
    private val notifier = ContactChangeNotifier(
        emailService,
        mock<AppConfigService>().also { whenever(it.getConfig()).thenReturn(AppConfig(name = "Example")) }
    )

    private fun user(email: String?, emailVerified: Boolean) = User(
        id = ObjectId.get(),
        firstName = "Ada",
        lastName = null,
        email = email,
        emailVerified = emailVerified,
        phoneNumber = "+9779800000000",
        phoneNumberVerified = true
    )

    @Test
    fun `a replaced email address is told at that address`() {
        notifier.emailReplaced(user("new@example.com", emailVerified = true), previous = "old@example.com")

        val model = argumentCaptor<Map<String, Any?>>()
        verify(emailService).sendHtmlMail(any(), eq("old@example.com"), any(), any(), model.capture())
        assertTrue("new@example.com" in model.firstValue["message"].toString())
    }

    @Test
    fun `a replaced phone number is told at the verified email address, and only there`() {
        notifier.phoneNumberReplaced(user("ada@example.com", emailVerified = true), previous = "+9779812345678")
        verify(emailService).sendHtmlMail(any(), eq("ada@example.com"), any(), any(), any())

        notifier.phoneNumberReplaced(user("unproven@example.com", emailVerified = false), previous = "+9779812345678")
        verify(emailService, never()).sendHtmlMail(any(), eq("unproven@example.com"), any(), any(), any())
    }
}
