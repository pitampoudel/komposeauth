package pitampoudel.komposeauth.app_config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.webauthn.utils.WebAuthnUtils.androidOrigin
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
class AppConfigServiceTest {

    @Autowired
    private lateinit var appConfigService: AppConfigService

    @Test
    fun `google client id and secret are selected by platform and blank becomes null`() {
        val provider = mock<AppConfigProvider>()
        whenever(provider.get()).thenReturn(
            AppConfig(
                googleAuthClientId = "web-id",
                googleAuthClientSecret = "web-secret",
                googleAuthDesktopClientId = "desktop-id",
                googleAuthDesktopClientSecret = "   ",
            )
        )

        val service = AppConfigService(provider)

        kotlin.test.assertEquals("desktop-id", service.googleClientId(Platform.DESKTOP))
        kotlin.test.assertEquals("web-id", service.googleClientId(Platform.WEB))
        assertNull(service.googleClientSecret(Platform.DESKTOP))
        kotlin.test.assertEquals("web-secret", service.googleClientSecret(Platform.ANDROID))
    }

    @Test
    fun `corsAllowedOrigins splits by comma and returns empty when null`() {
        val provider = mock<AppConfigProvider>()
        whenever(provider.get()).thenReturn(AppConfig(corsAllowedOriginList = null))
        val service = AppConfigService(provider)
        kotlin.test.assertEquals(emptyList(), service.corsAllowedOrigins())

        whenever(provider.get()).thenReturn(AppConfig(corsAllowedOriginList = "a,b"))
        kotlin.test.assertEquals(listOf("a", "b"), service.corsAllowedOrigins())
    }

    @Test
    fun `webauthnAllowedOrigins includes android origins plus cors origins`() {
        val provider = mock<AppConfigProvider>()
        whenever(provider.get()).thenReturn(
            AppConfig(
                allowedAndroidSha256List = "11:22,33:44",
                corsAllowedOriginList = "https://example.com"
            )
        )
        val service = AppConfigService(provider)

        val expected = setOf(
            androidOrigin("11:22"),
            androidOrigin("33:44"),
            "https://example.com"
        )

        kotlin.test.assertEquals(expected, service.webauthnAllowedOrigins())
    }


    @Test
    fun `saving and retrieving app config works`() {
        val config = AppConfig(
            googleAuthClientId = "test-client-id"
        )
        appConfigService.appConfigProvider.save(config)

        val retrievedConfig = appConfigService.googleClientId(Platform.WEB)
        assertEquals("test-client-id", retrievedConfig)
    }
}
