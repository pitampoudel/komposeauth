package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.user.entity.User

class AdminRoleChangeEmailNotifierTest {

    @Test
    fun `notify does nothing when user has no email`() {
        val emailService = mock<EmailService>()
        val appConfigProvider = mock<AppConfigProvider>()
        whenever(appConfigProvider.get()).thenReturn(
            AppConfig(
                name = "Test",
                websiteUrl = "https://example.com"
            )
        )
        val notifier = RoleChangeEmailNotifier(
            emailService = emailService,
            appConfigService = AppConfigService(appConfigProvider)
        )

        val target = User(
            id = ObjectId.get(),
            firstName = "No",
            lastName = "Email",
            email = null,
            phoneNumber = "+15555550100",
            passwordHash = "hash",
            roles = emptyList()
        )

        val ok = notifier.notify(target, RoleChangeEmailNotifier.Action.GRANTED, actor = null)
        assertFalse(ok)
        verifyNoInteractions(emailService)
    }

    @Test
    fun `notify sends email when user has email`() {
        val emailService = mock<EmailService>()
        whenever(
            emailService.sendHtmlMail(
                baseUrl = any(),
                to = any(),
                subject = any(),
                template = any(),
                model = any()
            )
        ).thenReturn(true)

        val appConfigProvider = mock<AppConfigProvider>()
        whenever(appConfigProvider.get()).thenReturn(
            AppConfig(
                name = "TestApp",
                websiteUrl = "https://example.com"
            )
        )

        val notifier = RoleChangeEmailNotifier(
            emailService = emailService,
            appConfigService = AppConfigService(appConfigProvider)
        )

        val target = User(
            id = ObjectId.get(),
            firstName = "Target",
            lastName = "User",
            email = "target@example.com",
            phoneNumber = null,
            passwordHash = "hash",
            roles = emptyList()
        )

        val ok = notifier.notify(target, RoleChangeEmailNotifier.Action.REVOKED, actor = "Test Admin")
        assertTrue(ok)

        verify(emailService).sendHtmlMail(
            baseUrl = eq("https://example.com"),
            to = eq("target@example.com"),
            subject = eq("Admin access revoked"),
            template = eq("email/generic"),
            model = any()
        )
    }
}
