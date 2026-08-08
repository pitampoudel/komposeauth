package pitampoudel.komposeauth.core.security

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationTrustResolverImpl
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PromptReAuthenticationFilterTest {

    private val securityContextRepository = HttpSessionSecurityContextRepository()
    private val filter = PromptReAuthenticationFilter("/oauth2/authorize", securityContextRepository)

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

    /**
     * ExceptionTranslationFilter only forwards to the login page when it considers the
     * authentication anonymous, so this — not a null authentication — is what makes the login
     * page appear instead of a 403.
     */
    private fun assertAnonymous() {
        val authentication = SecurityContextHolder.getContext().authentication
        assertTrue(
            authentication is AnonymousAuthenticationToken,
            "expected an anonymous authentication but was $authentication"
        )
        assertTrue(AuthenticationTrustResolverImpl().isAnonymous(authentication))
    }

    @Test
    fun `prompt=login drops the existing authentication`() {
        val request = authorizeRequest("login")
        authenticate(request)

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertAnonymous()
        assertFalse(sessionHoldsAuthentication(request))
    }

    @Test
    fun `prompt=select_account drops the existing authentication`() {
        val request = authorizeRequest("select_account")
        authenticate(request)

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertAnonymous()
        assertFalse(sessionHoldsAuthentication(request))
    }

    @Test
    fun `authorization request without prompt keeps the session`() {
        val request = authorizeRequest(prompt = null)
        authenticate(request)

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertNotNull(SecurityContextHolder.getContext().authentication)
        assertTrue(sessionHoldsAuthentication(request))
    }

    @Test
    fun `prompt on another endpoint is ignored`() {
        val request = MockHttpServletRequest("GET", "/oauth2/token").apply {
            setParameter("prompt", "login")
            session
        }
        authenticate(request)

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

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

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertNotNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `replayed authorization request keeps the freshly established authentication`() {
        val session = MockHttpSession()
        val first = authorizeRequest("login").apply { setSession(session) }
        authenticate(first)
        filter.doFilter(first, MockHttpServletResponse(), MockFilterChain())
        assertAnonymous()

        // The user logs back in and the saved authorization request — same state, same prompt —
        // is replayed against the same session.
        val replay = authorizeRequest("login").apply { setSession(session) }
        authenticate(replay)
        filter.doFilter(replay, MockHttpServletResponse(), MockFilterChain())

        assertNotNull(SecurityContextHolder.getContext().authentication)
        assertTrue(sessionHoldsAuthentication(replay))
    }

    @Test
    fun `a different authorization request is prompted again`() {
        val session = MockHttpSession()
        val first = authorizeRequest("login", state = "state-1").apply { setSession(session) }
        authenticate(first)
        filter.doFilter(first, MockHttpServletResponse(), MockFilterChain())

        val second = authorizeRequest("login", state = "state-2").apply { setSession(session) }
        authenticate(second)
        filter.doFilter(second, MockHttpServletResponse(), MockFilterChain())

        assertAnonymous()
    }

    @Test
    fun `re-authentication is requested only for login and select_account`() {
        assertTrue(PromptReAuthenticationFilter.requestsReAuthentication("login"))
        assertTrue(PromptReAuthenticationFilter.requestsReAuthentication("select_account"))
        assertTrue(PromptReAuthenticationFilter.requestsReAuthentication("consent select_account"))
        assertFalse(PromptReAuthenticationFilter.requestsReAuthentication("consent"))
        assertFalse(PromptReAuthenticationFilter.requestsReAuthentication("none"))
        assertFalse(PromptReAuthenticationFilter.requestsReAuthentication(""))
        assertFalse(PromptReAuthenticationFilter.requestsReAuthentication(null))
    }
}
