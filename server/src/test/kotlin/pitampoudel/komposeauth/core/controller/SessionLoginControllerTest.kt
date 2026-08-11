package pitampoudel.komposeauth.core.controller

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.ui.ExtendedModelMap
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionLoginControllerTest {

    private val appConfigService: AppConfigService = mock()
    private val controller = SessionLoginController(appConfigService)

    private fun googleConfigured(configured: Boolean = true) {
        whenever(appConfigService.getConfig()).thenReturn(
            AppConfig(
                googleAuthClientId = if (configured) "client-id" else null,
                googleAuthClientSecret = if (configured) "client-secret" else null
            )
        )
    }

    /** Saves an authorization request the way the login entry point does before redirecting here. */
    private fun sessionWithAuthorizationRequest(vararg parameters: Pair<String, String>): MockHttpSession {
        val session = MockHttpSession()
        val authorizationRequest = MockHttpServletRequest("GET", "/oauth2/authorize").apply {
            setSession(session)
            parameters.forEach { (name, value) -> setParameter(name, value) }
        }
        HttpSessionRequestCache().saveRequest(authorizationRequest, MockHttpServletResponse())
        return session
    }

    private fun loginPage(session: MockHttpSession?, error: String? = null): String =
        controller.loginPage(
            error = error,
            request = MockHttpServletRequest("GET", "/session-login").apply { session?.let { setSession(it) } },
            response = MockHttpServletResponse(),
            model = ExtendedModelMap()
        )

    @Test
    fun `idp=google on the authorization request skips the form`() {
        googleConfigured()

        val view = loginPage(sessionWithAuthorizationRequest("idp" to "google"))

        assertEquals("redirect:/oauth2/authorization/google", view)
    }

    @Test
    fun `login_hint is carried through to google`() {
        googleConfigured()

        val view = loginPage(
            sessionWithAuthorizationRequest("idp" to "google", "login_hint" to "someone@example.com")
        )

        assertEquals(
            "redirect:/oauth2/authorization/google?login_hint=someone@example.com",
            view
        )
    }

    @Test
    fun `a failed provider login shows the form instead of bouncing back to google`() {
        googleConfigured()

        val view = loginPage(sessionWithAuthorizationRequest("idp" to "google"), error = "provider")

        assertEquals("session-login", view)
    }

    @Test
    fun `no idp hint renders the form`() {
        googleConfigured()

        assertEquals("session-login", loginPage(sessionWithAuthorizationRequest()))
        assertEquals("session-login", loginPage(session = null))
    }

    @Test
    fun `idp=google is ignored when google is not configured`() {
        googleConfigured(configured = false)

        val view = loginPage(sessionWithAuthorizationRequest("idp" to "google"))

        assertEquals("session-login", view)
    }
}
