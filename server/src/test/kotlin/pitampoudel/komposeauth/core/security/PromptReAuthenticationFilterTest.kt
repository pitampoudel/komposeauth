package pitampoudel.komposeauth.core.security

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PromptReAuthenticationFilterTest {

    private val securityContextRepository = HttpSessionSecurityContextRepository()
    private val filter = PromptReAuthenticationFilter(
        securityContextRepository,
        LoginUrlAuthenticationEntryPoint("/session-login")
    )

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    private fun authorizeRequest(prompt: String?, state: String? = "state-1"): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/oauth2/authorize").apply {
            queryString = "response_type=code&client_id=next-app"
            prompt?.let { setParameter("prompt", it) }
            state?.let { setParameter("state", it) }
            session // force a session so the security context has somewhere to live
        }

    private fun authenticate(request: MockHttpServletRequest) {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = UsernamePasswordAuthenticationToken(
            "user-id", null, AuthorityUtils.createAuthorityList("ROLE_USER")
        )
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, MockHttpServletResponse())
    }

    private fun sessionHoldsAuthentication(request: MockHttpServletRequest): Boolean =
        request.session?.getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        ) != null

    @Test
    fun `prompt=login sends the user back to the login page`() {
        val request = authorizeRequest("login")
        authenticate(request)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals("/session-login", response.redirectedUrl)
        assertNull(SecurityContextHolder.getContext().authentication)
        assertFalse(sessionHoldsAuthentication(request))
    }

    @Test
    fun `the authorization request is saved so it can be replayed after login`() {
        val request = authorizeRequest("login")
        authenticate(request)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        val saved = assertNotNull(HttpSessionRequestCache().getRequest(request, response))
        assertTrue(saved.redirectUrl.contains("/oauth2/authorize"))
    }

    @Test
    fun `prompt=select_account sends the user back to the login page`() {
        val request = authorizeRequest("select_account")
        authenticate(request)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals("/session-login", response.redirectedUrl)
        assertFalse(sessionHoldsAuthentication(request))
    }

    @Test
    fun `authorization request without prompt keeps the session`() {
        val request = authorizeRequest(prompt = null)
        authenticate(request)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(response.redirectedUrl)
        assertNotNull(SecurityContextHolder.getContext().authentication)
        assertTrue(sessionHoldsAuthentication(request))
    }

    @Test
    fun `prompt=none is left to the authorization server`() {
        val request = authorizeRequest("none")
        authenticate(request)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(response.redirectedUrl)
        assertNotNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `prompt on another endpoint is ignored`() {
        val request = MockHttpServletRequest("GET", "/oauth2/token").apply {
            setParameter("prompt", "login")
            session
        }
        authenticate(request)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(response.redirectedUrl)
        assertNotNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `anonymous authentication is left alone`() {
        val request = authorizeRequest("login")
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = AnonymousAuthenticationToken(
            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        )
        SecurityContextHolder.setContext(context)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(response.redirectedUrl)
        assertNotNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `replayed authorization request keeps the freshly established authentication`() {
        val session = MockHttpSession()
        val first = authorizeRequest("login").apply { setSession(session) }
        authenticate(first)
        filter.doFilter(first, MockHttpServletResponse(), MockFilterChain())

        // The user logs back in and the saved authorization request — same state, same prompt —
        // is replayed against the same session.
        val replay = authorizeRequest("login").apply { setSession(session) }
        authenticate(replay)
        val response = MockHttpServletResponse()
        filter.doFilter(replay, response, MockFilterChain())

        assertNull(response.redirectedUrl)
        assertNotNull(SecurityContextHolder.getContext().authentication)
        assertTrue(sessionHoldsAuthentication(replay))
    }

    /**
     * Two tabs signing in at once. The second one to reach the login page must not cost the first
     * one its place: both replays have been through the page, so neither is sent out again.
     */
    @Test
    fun `sign-ins in flight alongside each other are each replayed once`() {
        val session = MockHttpSession()
        listOf("tab-a", "tab-b").forEach { state ->
            val request = authorizeRequest("login", state = state).apply { setSession(session) }
            authenticate(request)
            filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())
        }

        val replayA = authorizeRequest("login", state = "tab-a").apply { setSession(session) }
        authenticate(replayA)
        val responseA = MockHttpServletResponse()
        filter.doFilter(replayA, responseA, MockFilterChain())

        assertNull(responseA.redirectedUrl, "the first tab was sent out to sign in a second time")
        assertTrue(sessionHoldsAuthentication(replayA))
    }

    @Test
    fun `a different authorization request is prompted again`() {
        val session = MockHttpSession()
        val first = authorizeRequest("login", state = "state-1").apply { setSession(session) }
        authenticate(first)
        filter.doFilter(first, MockHttpServletResponse(), MockFilterChain())

        val second = authorizeRequest("login", state = "state-2").apply { setSession(session) }
        authenticate(second)
        val response = MockHttpServletResponse()
        filter.doFilter(second, response, MockFilterChain())

        assertEquals("/session-login", response.redirectedUrl)
    }
}
