package pitampoudel.komposeauth.app_config

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.webauthn.utils.WebAuthnUtils.androidOrigin
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppConfigServiceTest {

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

        assertEquals("desktop-id", service.googleClientId(Platform.DESKTOP))
        assertEquals("web-id", service.googleClientId(Platform.WEB))
        assertNull(service.googleClientSecret(Platform.DESKTOP))
        assertEquals("web-secret", service.googleClientSecret(Platform.ANDROID))
    }

    @Test
    fun `corsAllowedOrigins splits by comma and returns empty when null`() {
        val provider = mock<AppConfigProvider>()
        whenever(provider.get()).thenReturn(AppConfig(corsAllowedOriginList = null))
        val service = AppConfigService(provider)
        assertEquals(emptyList(), service.corsAllowedOrigins())

        whenever(provider.get()).thenReturn(AppConfig(corsAllowedOriginList = "a,b"))
        assertEquals(listOf("a", "b"), service.corsAllowedOrigins())
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

        assertEquals(expected, service.webauthnAllowedOrigins())
    }
}

