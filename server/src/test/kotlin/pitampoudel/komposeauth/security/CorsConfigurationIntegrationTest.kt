package pitampoudel.komposeauth.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.get
import org.springframework.web.cors.CorsConfigurationSource
import pitampoudel.komposeauth.TestConfig
import kotlin.test.assertTrue

/**
 * Guards against the server refusing to talk to itself, and against allowing anyone else.
 *
 * Both failures ended as a bare "Invalid CORS request" in the browser with nothing to say which
 * setting caused it, and both locked the operator out of the configuration page — the one place the
 * setting could have been corrected.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class CorsConfigurationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var corsConfigurationSource: CorsConfigurationSource

    @Test
    fun `a request from the server's own origin is never refused`() {
        // Browsers attach Origin to same-origin POSTs, and Spring compares scheme, host and port to
        // decide whether that is cross-origin. Behind a proxy terminating TLS, the server can
        // believe it is on http while the page says https — which made the console refuse its own
        // form posts. Matching on host alone settles it.
        val response = mockMvc.get("/session-login") {
            header("Origin", "https://localhost")
        }.andReturn().response

        assertTrue(
            response.status !in 400..499,
            "the server refused its own origin (status ${response.status}): ${response.contentAsString}"
        )
        assertTrue(
            !response.contentAsString.contains("Invalid CORS request"),
            "expected no CORS rejection, got: ${response.contentAsString}"
        )
    }

    @Test
    fun `an origin that names neither the server nor an allowed host is still not ours`() {
        // Nothing is added to the allow-list at request time, so an Origin header can never talk
        // its way onto it.
        val request = MockHttpServletRequest("GET", "/users").apply {
            serverName = "auth.example.com"
            addHeader("Host", "auth.example.com")
            addHeader("Origin", "https://evil.example.com")
        }

        assertTrue(
            "https://evil.example.com" !in allowedOriginsFor(request),
            "a foreign origin was reflected back as allowed"
        )
    }

    @Test
    fun `no configured origins means no opinion, not refuse everyone`() {
        // Nothing is configured in the test profile, which is the state of a fresh install. An empty
        // allow-list used to mean "refuse every origin", so the first request carrying an Origin --
        // including the config page's own save -- came back 403 before anything could be set up.
        val response = mockMvc.get("/session-login") {
            header("Origin", "https://somewhere-else.example.com")
        }.andReturn().response

        // Refused by the browser for want of Allow-Origin, which is correct. Not refused by us.
        assertTrue(
            !response.contentAsString.contains("Invalid CORS request"),
            "unconfigured CORS must not hard-refuse; got: ${response.contentAsString}"
        )
    }

    private fun allowedOriginsFor(request: MockHttpServletRequest): List<String> {
        val configuration = corsConfigurationSource.getCorsConfiguration(request) ?: return emptyList()
        return configuration.allowedOrigins.orEmpty() + configuration.allowedOriginPatterns.orEmpty()
    }
}
