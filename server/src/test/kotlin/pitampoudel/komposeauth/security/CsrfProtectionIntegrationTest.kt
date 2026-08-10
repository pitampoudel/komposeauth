package pitampoudel.komposeauth.security

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The rest of the suite gets a CSRF token on every request from [TestConfig], which is what a real
 * browser would have. These tests cover the other half: that a cookie-authenticated, state-changing
 * request without one is actually turned away.
 *
 * This matters because the access-token cookie is deliberately SameSite=None, so it rides along on
 * cross-site requests. Without this protection any page on the internet could drive a write using a
 * signed-in victim's credentials.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class CsrfProtectionIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var securityFilterChainProxy: FilterChainProxy

    /**
     * What the live CsrfFilter thinks about a request to [path]: whether it considers protection
     * required, and which repository it is consulting. Both are set at configuration time, so if
     * either is unexpected the cause is in WebSecurityConfig rather than in the request.
     */
    private fun csrfFilterState(path: String): String {
        val filter = securityFilterChainProxy.getFilters(path).orEmpty()
            .filterIsInstance<CsrfFilter>()
            .firstOrNull() ?: return "no CsrfFilter in chain"
        val probe = MockHttpServletRequest("POST", path).apply {
            requestURI = path
            servletPath = path
        }
        val matcher = ReflectionTestUtils.getField(filter, "requireCsrfProtectionMatcher") as? RequestMatcher
        val repository = ReflectionTestUtils.getField(filter, "tokenRepository")
        val handler = ReflectionTestUtils.getField(filter, "requestHandler")
        return "requiresProtection=${matcher?.matches(probe)}, " +
                "repository=${repository?.javaClass?.simpleName}, " +
                "handler=${handler?.javaClass?.simpleName}"
    }

    /** The security filters actually assembled for [path], in order. */
    private fun filtersFor(path: String): List<String> =
        securityFilterChainProxy.getFilters(path).orEmpty().map { it.javaClass.simpleName }

    @Test
    fun `csrf filter is present in the chain that serves the api`() {
        // Checked on its own so a missing filter reports itself directly, rather than showing up
        // as the confusing "a forged write succeeded" further down.
        val filters = filtersFor("/${ApiEndpoints.UPDATE_PROFILE}")
        assertTrue(
            filters.any { it == "CsrfFilter" },
            "CsrfFilter is not in the chain for /${ApiEndpoints.UPDATE_PROFILE}; chain was: $filters"
        )
    }

    @Test
    fun `cookie-authenticated write is rejected without a valid csrf token`() {
        val email = "csrf-reject@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        // The same fully-wired client the rest of the suite uses, so the chain under test is the
        // real one — but opting out of the automatic token, so this request carries none at all.
        //
        // An earlier version sent a deliberately invalid token instead. That does not work: csrf()
        // *sets* the token parameter, so the harness's own token processor simply overwrote the bad
        // value with a good one and the write sailed through, looking exactly like a security hole.
        // Sending nothing cannot be undone by ordering.
        val result = mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            header(TestConfig.OMIT_CSRF_TOKEN_HEADER, "true")
            cookie(cookie)
            content = """{"givenName":"Forged"}"""
        }.andReturn()
        val response = result.response

        // Did CsrfFilter actually execute for THIS request? It sets this attribute as its first
        // action, before any decision, so its absence means the filter never ran — which would be a
        // different problem entirely from the filter running and letting the request past.
        val csrfFilterRan =
            result.request.getAttribute("org.springframework.security.web.csrf.DeferredCsrfToken") != null
        // And what the live matcher says about the real request, rather than a hand-made stand-in.
        val requiresProtectionHere = (
            securityFilterChainProxy.getFilters("/${ApiEndpoints.UPDATE_PROFILE}").orEmpty()
                .filterIsInstance<CsrfFilter>().firstOrNull()
                ?.let { ReflectionTestUtils.getField(it, "requireCsrfProtectionMatcher") as? RequestMatcher }
                ?.matches(result.request)
            )

        // Checked first, because this is the property the protection exists for: whatever status
        // the refusal is dressed up as, the forged write must not have landed.
        val user = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertNotEquals(
            "Forged",
            user.firstName,
            "forged write was applied — CSRF is not protecting this endpoint " +
                    "(status ${response.status}). " +
                    "csrfFilterRan=$csrfFilterRan, " +
                    "requiresProtectionForThisRequest=$requiresProtectionHere, " +
                    "resolvedException=${result.resolvedException}, " +
                    "dispatcherType=${result.request.dispatcherType}. " +
                    "Filter state: ${csrfFilterState("/${ApiEndpoints.UPDATE_PROFILE}")}"
        )

        // Deliberately only "not success". CsrfFilter runs before the bearer token in the cookie is
        // read, so the principal is still anonymous when the request is refused, and
        // ExceptionTranslationFilter turns an anonymous access denial into "start authentication" —
        // which lands as a 401 or, for a client the entry point decides to redirect, a 302. Pinning
        // an exact code here tests Spring's error plumbing rather than this application's security.
        assertTrue(
            response.status !in 200..299,
            "forged write should not have succeeded, got ${response.status}"
        )
    }

    @Test
    fun `same write succeeds with a valid csrf token`() {
        val email = "csrf-accept@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.UPDATE_PROFILE}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = """{"givenName":"Legitimate"}"""
        }.andExpect {
            status { isOk() }
        }

        val user = userRepository.findById(ObjectId(userId)).orElseThrow()
        assertNotEquals("Forged", user.firstName)
    }
}
