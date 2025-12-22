package pitampoudel.komposeauth.core.config

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.security.WebAuthorizationConfig
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BearerTokenResolverTest {

    private val resolver = WebAuthorizationConfig().bearerTokenResolver()

    @Test
    fun `prefers Authorization header over cookie`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer header-token")
            setCookies(Cookie(ACCESS_TOKEN_COOKIE_NAME, "cookie-token"))
        }
        assertEquals("header-token", resolver.resolve(request))
    }

    @Test
    fun `falls back to cookie when Authorization header missing`() {
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie(ACCESS_TOKEN_COOKIE_NAME, "cookie-token"))
        }
        assertEquals("cookie-token", resolver.resolve(request))
    }

    @Test
    fun `malformed Authorization header does not throw and falls back to cookie`() {
        val request = MockHttpServletRequest().apply {
            // Causes DefaultBearerTokenResolver to throw (invalid format)
            addHeader("Authorization", "Bearer")
            setCookies(Cookie(ACCESS_TOKEN_COOKIE_NAME, "cookie-token"))
        }
        assertEquals("cookie-token", resolver.resolve(request))
    }

    @Test
    fun `returns null when neither header nor cookie present`() {
        val request = MockHttpServletRequest()
        assertNull(resolver.resolve(request))
    }
}

