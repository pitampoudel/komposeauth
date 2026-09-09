package pitampoudel.komposeauth.core.security

import org.springframework.mock.web.MockHttpServletRequest
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Which public endpoints may be called without a CSRF token.
 *
 * The list is easy to get wrong in the quiet direction: an endpoint that is public for
 * authentication but absent here simply answers 403 to every caller that has no token to send, and
 * says nothing about why.
 */
class CsrfExemptionsTest {

    private val exempt = PublicEndpoints.csrfExemptRequestMatcher()

    private fun isExempt(method: String, path: String) =
        exempt.matches(MockHttpServletRequest(method, path))

    @Test
    fun `asking for a one-time code needs no token`() {
        // A signup step, taken before the caller has any token; a browser app doing it carries
        // cookies, so it is not a header-only bearer request either.
        assertTrue(isExempt("POST", "/${ApiEndpoints.SEND_OTP}"))
    }

    @Test
    fun `verifying one still does`() {
        // Authenticated, and it attaches the address to the caller's account.
        assertFalse(isExempt("POST", "/${ApiEndpoints.VERIFY_OTP}"))
    }

    @Test
    fun `signing in through the browser page still does`() {
        assertFalse(isExempt("POST", "/session-login"))
    }

    @Test
    fun `signing out still does`() {
        assertFalse(isExempt("POST", "/${ApiEndpoints.LOGOUT}"))
    }

    @Test
    fun `saving configuration still does`() {
        // The page reads and writes every secret this server holds.
        assertFalse(isExempt("POST", "/admin/config"))
    }

    @Test
    fun `writing users still does`() {
        assertFalse(isExempt("PATCH", "/${ApiEndpoints.USERS}"))
    }

    @Test
    fun `the JSON login API stays exempt`() {
        assertTrue(isExempt("POST", "/${ApiEndpoints.LOGIN}"))
    }
}
