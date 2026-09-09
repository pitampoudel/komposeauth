package pitampoudel.komposeauth.core.security

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockHttpServletRequest
import pitampoudel.komposeauth.app_config.service.AppConfigService
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Which origins the server treats as its own.
 *
 * The console's form posts carry an `Origin`, and Spring's CORS processor refuses a non-matching
 * one outright rather than merely omitting the header — so getting this wrong locks an operator out
 * of the configuration page while any allow-list is set. It went wrong exactly where `serverName`
 * is not the name the browser used, which is a property of the deployment's proxy rather than of
 * the build, hence the same jar behaving differently in different places.
 */
class OwnOriginCorsTest {

    private fun sourceWithConfiguredOrigins(vararg configured: String) =
        WebSecurityConfig().corsConfigurationSource(
            mock<AppConfigService> { on { corsAllowedOrigins() } doReturn configured.toList() }
        )

    private fun allowedFor(
        request: MockHttpServletRequest,
        vararg configured: String
    ): List<String> {
        val configuration = sourceWithConfiguredOrigins(*configured)
            .getCorsConfiguration(request) ?: return emptyList()
        return configuration.allowedOrigins.orEmpty() + configuration.allowedOriginPatterns.orEmpty()
    }

    private fun request(
        serverName: String,
        host: String?,
        forwardedHost: String? = null,
        origin: String
    ) = MockHttpServletRequest("POST", "/admin/config").apply {
        this.serverName = serverName
        host?.let { addHeader("Host", it) }
        forwardedHost?.let { addHeader("X-Forwarded-Host", it) }
        addHeader("Origin", origin)
    }

    @Test
    fun `serverName names us`() {
        val allowed = allowedFor(
            request(serverName = "auth.example.com", host = null, origin = "https://auth.example.com"),
            "https://app.example.com"
        )

        assertTrue("https://auth.example.com" in allowed, "allowed: $allowed")
    }

    @Test
    fun `the Host header names us when serverName does not`() {
        // A proxy that rewrites neither, or FORWARD_HEADERS_STRATEGY set to none: serverName is the
        // container's own name and only the Host header says where the browser went.
        val allowed = allowedFor(
            request(
                serverName = "internal-container.local",
                host = "auth.example.com:443",
                origin = "https://auth.example.com"
            ),
            "https://app.example.com"
        )

        assertTrue("https://auth.example.com" in allowed, "allowed: $allowed")
    }

    @Test
    fun `X-Forwarded-Host names us when neither of the others does`() {
        val allowed = allowedFor(
            request(
                serverName = "internal-container.local",
                host = "internal-container.local",
                forwardedHost = "auth.example.com, inner-proxy.local",
                origin = "https://auth.example.com"
            ),
            "https://app.example.com"
        )

        assertTrue("https://auth.example.com" in allowed, "allowed: $allowed")
    }

    @Test
    fun `a foreign origin is never reflected`() {
        val allowed = allowedFor(
            request(
                serverName = "auth.example.com",
                host = "auth.example.com",
                origin = "https://evil.example.com"
            ),
            "https://app.example.com"
        )

        assertFalse("https://evil.example.com" in allowed, "allowed: $allowed")
    }

    @Test
    fun `a configured origin is still allowed`() {
        val allowed = allowedFor(
            request(
                serverName = "auth.example.com",
                host = "auth.example.com",
                origin = "https://app.example.com"
            ),
            "https://app.example.com"
        )

        assertTrue("https://app.example.com" in allowed, "allowed: $allowed")
    }

    @Test
    fun `nothing configured and nothing of ours means no opinion, not refuse everyone`() {
        val configuration = sourceWithConfiguredOrigins()
            .getCorsConfiguration(
                request(
                    serverName = "auth.example.com",
                    host = "auth.example.com",
                    origin = "https://somewhere-else.example.com"
                )
            )

        assertTrue(configuration == null, "an unconfigured server must not hard-refuse an origin")
    }
}
