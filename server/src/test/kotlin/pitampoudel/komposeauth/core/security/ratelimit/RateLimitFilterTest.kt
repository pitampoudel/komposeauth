package pitampoudel.komposeauth.core.security.ratelimit

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import java.time.Clock
import kotlin.test.assertEquals

/**
 * Who a request is counted as. Everybody behind one office's address shares that address's budget,
 * so the OTP endpoints count a signed-in caller by account instead.
 */
class RateLimitFilterTest {

    private val properties = RateLimitProperties()

    private val filter = RateLimitFilter(
        rateLimiter = RateLimiter(InMemoryRateLimitStore(), Clock.systemUTC()),
        properties = properties,
        clientIpResolver = mock { on { resolve(any()) } doReturn "203.0.113.9" },
        bearerTokenResolver = DefaultBearerTokenResolver()
    )

    private fun tokenFor(subject: String) = PlainJWT(JWTClaimsSet.Builder().subject(subject).build()).serialize()

    private fun status(method: String, path: String, bearer: String? = null): Int {
        val request = MockHttpServletRequest(method, path).apply {
            bearer?.let { addHeader("Authorization", "Bearer $it") }
        }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response.status
    }

    @Test
    fun `two accounts behind one address each have their own budget for codes`() {
        val limit = properties.otpSend.limit
        repeat(limit) { assertEquals(200, status("POST", "/${ApiEndpoints.SEND_OTP}", tokenFor("alice"))) }
        assertEquals(429, status("POST", "/${ApiEndpoints.SEND_OTP}", tokenFor("alice")))

        assertEquals(200, status("POST", "/${ApiEndpoints.SEND_OTP}", tokenFor("bob")))
        assertEquals(200, status("POST", "/${ApiEndpoints.VERIFY_OTP}", tokenFor("alice")))
    }

    @Test
    fun `without a token the address is what is counted`() {
        val limit = properties.otpVerify.limit
        repeat(limit) { assertEquals(200, status("POST", "/${ApiEndpoints.VERIFY_OTP}")) }
        assertEquals(429, status("POST", "/${ApiEndpoints.VERIFY_OTP}"))
        // Something that is not a token reads as no token.
        assertEquals(429, status("POST", "/${ApiEndpoints.VERIFY_OTP}", bearer = "not-a-jwt"))
        assertEquals(200, status("POST", "/${ApiEndpoints.VERIFY_OTP}", tokenFor("carol")))
    }

    @Test
    fun `sign-in stays counted by address whatever token is sent`() {
        val limit = properties.login.limit
        repeat(limit) { i -> assertEquals(200, status("POST", "/${ApiEndpoints.LOGIN}", tokenFor("user-$i"))) }
        assertEquals(429, status("POST", "/${ApiEndpoints.LOGIN}", tokenFor("someone-new")))
    }

    @Test
    fun `login options are limited per address`() {
        val limit = properties.loginOptions.limit
        repeat(limit) { assertEquals(200, status("GET", "/${ApiEndpoints.LOGIN_OPTIONS}")) }
        assertEquals(429, status("GET", "/${ApiEndpoints.LOGIN_OPTIONS}"))
    }
}
