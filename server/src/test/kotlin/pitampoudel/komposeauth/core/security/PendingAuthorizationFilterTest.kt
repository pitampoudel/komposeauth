package pitampoudel.komposeauth.core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What the filter records and what it drops, which is the whole of it: the root trusts the slot to
 * mean "this visitor is in the middle of somebody's sign-in and has not finished it".
 */
class PendingAuthorizationFilterTest {

    private val filter = PendingAuthorizationFilter("/session-login")
    private val query = "response_type=code&client_id=abc&state=s1"

    private fun authorizeRequest(method: String = "GET") = MockHttpServletRequest(method, "/oauth2/authorize").apply {
        queryString = query
        // The chain has a session by the time it answers: the saved request needs one.
        getSession(true)
    }

    /** A chain that ends the way the real one does when it sends the visitor somewhere. */
    private fun redirectingTo(location: String?) = FilterChain { _, response ->
        location?.let { (response as HttpServletResponse).sendRedirect(it) }
    }

    @Test
    fun `an authorization request sent to the login page is kept, query and all`() {
        val request = authorizeRequest()
        filter.doFilter(request, MockHttpServletResponse(), redirectingTo("http://localhost/session-login"))

        assertEquals("/oauth2/authorize?$query", PendingAuthorizationFilter.take(request))
    }

    @Test
    fun `one that reaches the relying party is finished, and leaves nothing behind`() {
        val request = authorizeRequest()
        filter.doFilter(request, MockHttpServletResponse(), redirectingTo("http://localhost/session-login"))
        // The same session comes back once they have signed in, and this time gets its code.
        filter.doFilter(request, MockHttpServletResponse(), redirectingTo("https://rp.example.com/cb?code=x"))

        assertNull(
            PendingAuthorizationFilter.take(request),
            "a finished sign-in would send the next visit to the root back through the endpoint"
        )
    }

    @Test
    fun `an authorization request answered with a page is finished too`() {
        val request = authorizeRequest()
        filter.doFilter(request, MockHttpServletResponse(), redirectingTo("http://localhost/session-login"))
        filter.doFilter(request, MockHttpServletResponse(), redirectingTo(null))

        assertNull(PendingAuthorizationFilter.take(request))
    }

    /** The consent form. Replaying it as a browser navigation would drop the body it carried. */
    @Test
    fun `a posted authorization request is not remembered`() {
        val request = authorizeRequest(method = "POST")
        filter.doFilter(request, MockHttpServletResponse(), redirectingTo("http://localhost/session-login"))

        assertNull(PendingAuthorizationFilter.take(request))
    }

    @Test
    fun `nothing else on the endpoint's chain is touched`() {
        val request = MockHttpServletRequest("GET", "/oauth2/token").apply { getSession(true) }
        filter.doFilter(request, MockHttpServletResponse(), redirectingTo("http://localhost/session-login"))

        assertNull(PendingAuthorizationFilter.take(request))
    }

    @Test
    fun `it is taken once, so a failed resume is not retried forever`() {
        val request = authorizeRequest()
        filter.doFilter(request, MockHttpServletResponse(), redirectingTo("/session-login"))

        assertEquals("/oauth2/authorize?$query", PendingAuthorizationFilter.take(request))
        assertNull(PendingAuthorizationFilter.take(request))
    }

    @Test
    fun `a visitor with no session has nothing waiting`() {
        assertNull(PendingAuthorizationFilter.take(MockHttpServletRequest("GET", "/")))
    }
}
