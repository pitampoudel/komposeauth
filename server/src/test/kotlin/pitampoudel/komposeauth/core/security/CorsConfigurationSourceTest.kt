package pitampoudel.komposeauth.core.security

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.cors.CorsUtils
import pitampoudel.komposeauth.app_config.service.AppConfigService
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the server allows across origins, and who decides it.
 *
 * The allow-list is the operator's, and nothing is added to it at request time. Two earlier attempts
 * to be helpful here went wrong in both directions: reflecting the request's own origin after a
 * check locked operators out when the check misread the deployment, and reflecting it without one
 * allowed every origin on the internet with credentials. Spring settles the case those were for —
 * see the same-origin test below.
 */
class CorsConfigurationSourceTest {

    private fun configurationFor(vararg configured: String) =
        WebSecurityConfig().corsConfigurationSource(
            mock<AppConfigService> { on { corsAllowedOrigins() } doReturn configured.toList() }
        ).getCorsConfiguration(MockHttpServletRequest("POST", "/users"))

    private fun allowedOrigins(vararg configured: String): List<String> {
        val configuration = configurationFor(*configured) ?: return emptyList()
        return configuration.allowedOrigins.orEmpty() + configuration.allowedOriginPatterns.orEmpty()
    }

    @Test
    fun `a configured origin is allowed`() {
        assertTrue("https://app.example.com" in allowedOrigins("https://app.example.com"))
    }

    @Test
    fun `a wildcard origin is expressed as a pattern`() {
        // allowedOrigins rejects patterns outright when credentials are allowed; allowedOriginPatterns
        // is the field that accepts them.
        val configuration = assertNotNull(configurationFor("https://*.example.com"))

        assertTrue("https://*.example.com" in configuration.allowedOriginPatterns.orEmpty())
        assertTrue(configuration.allowedOrigins.isNullOrEmpty())
    }

    @Test
    fun `nothing is added to the allow-list at request time`() {
        // The regression that matters. Adding the request's own Origin header here reflects whatever
        // arrives back in Access-Control-Allow-Origin, and credentials are allowed, so any page on
        // the internet could read authenticated responses on a signed-in visitor's behalf.
        assertFalse("https://evil.example.com" in allowedOrigins("https://app.example.com"))
    }

    @Test
    fun `an unconfigured server has no opinion rather than refusing everyone`() {
        assertNull(configurationFor())
    }

    @Test
    fun `Spring does not treat a same-origin request as cross-origin`() {
        // What makes the allow-list safe to leave to the operator: the console's own form posts
        // carry an Origin, and Spring compares its scheme, host and port to the request's own and
        // reports false, so DefaultCorsProcessor never reaches the allow-list at all. Pinned because
        // the CORS bean's correctness rests on it.
        val sameOrigin = MockHttpServletRequest("POST", "/admin/config").apply {
            scheme = "https"
            serverName = "auth.example.com"
            serverPort = 443
            addHeader("Origin", "https://auth.example.com")
        }

        assertFalse(CorsUtils.isCorsRequest(sameOrigin))
    }

    @Test
    fun `Spring does treat another site as cross-origin`() {
        val crossOrigin = MockHttpServletRequest("POST", "/admin/config").apply {
            scheme = "https"
            serverName = "auth.example.com"
            serverPort = 443
            addHeader("Origin", "https://evil.example.com")
        }

        assertTrue(CorsUtils.isCorsRequest(crossOrigin))
    }
}
