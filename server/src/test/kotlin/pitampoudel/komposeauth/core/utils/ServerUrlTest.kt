package pitampoudel.komposeauth.core.utils

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerUrlTest {

    private fun request() = MockHttpServletRequest().apply {
        scheme = "https"
        serverName = "attacker.example"
        serverPort = 443
    }

    @Test
    fun `the configured issuer wins over whatever host the request names`() {
        val serverUrl = ServerUrl(AuthorizationServerSettings.builder().issuer("https://auth.example.com").build())
        assertEquals("https://auth.example.com", serverUrl.of(request()))
    }

    @Test
    fun `unset, it is read off the request`() {
        val serverUrl = ServerUrl(AuthorizationServerSettings.builder().build())
        assertEquals("https://attacker.example", serverUrl.of(request()))
        assertEquals(
            "http://localhost:8080",
            serverUrl.of(MockHttpServletRequest().apply { serverName = "localhost"; serverPort = 8080 })
        )
    }
}
