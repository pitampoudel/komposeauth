package pitampoudel.komposeauth.core.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.utils.ServerUrl
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Google is told where to send the visitor back, and the code exchange repeats it; neither is
 * worked out from the Host the request to start the sign-in arrived with.
 */
class OAuthClientConfigTest {

    private val config = OAuthClientConfig()
    private val appConfigService = mockk<AppConfigService> {
        every { getConfig() } returns AppConfig(googleAuthClientId = "client-id", googleAuthClientSecret = "client-secret")
    }
    private val repository = config.clientRegistrationRepository(
        appConfigService,
        ServerUrl(AuthorizationServerSettings.builder().issuer("https://auth.example.com").build())
    )

    @Test
    fun `the redirect uri is the configured address whatever host the request named`() {
        val request = MockHttpServletRequest("GET", "/oauth2/authorization/google").apply {
            scheme = "https"
            serverName = "attacker.example"
            serverPort = 443
            addHeader("Host", "attacker.example")
        }

        val authorizationRequest = assertNotNull(config.authorizationRequestResolver(repository).resolve(request))

        assertEquals("https://auth.example.com/login/oauth2/code/google", authorizationRequest.redirectUri)
    }
}
