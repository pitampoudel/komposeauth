package pitampoudel.komposeauth.security

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import pitampoudel.komposeauth.core.security.WebSecurityConfig
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * What the request cache is allowed to remember across a sign-in.
 *
 * The saved request is a single slot in the session, and the last unauthenticated request wins it.
 * That makes the rules below load-bearing rather than cosmetic: a visitor sent here by a relying
 * party, who then opens the root in another tab before filling in the login page, had their
 * authorization request replaced by `/` and finished the sign-in on the landing page while the
 * relying party heard nothing. `a detour to the root does not lose the relying party` drives that
 * whole trail over HTTP; this pins the rule it depends on.
 */
class SavedRequestRulesTest {

    private val cache = WebSecurityConfig().requestCache()

    @Test
    fun `an authorization request is remembered`() {
        assertNotNull(
            save(HttpMethod.GET, "/oauth2/authorize", query = "response_type=code&client_id=abc"),
            "the request that carries a visitor back to their relying party was not saved"
        )
    }

    @Test
    fun `the root is not remembered`() {
        assertNull(
            save(HttpMethod.GET, "/"),
            "the root displaced whatever the sign-in was actually for; it is where an empty cache " +
                    "already goes, so saving it can only cost"
        )
    }

    @Test
    fun `the root with a query is not remembered either`() {
        assertNull(save(HttpMethod.GET, "/", query = "continue"), "the sign-in redirect comes back here")
    }

    @Test
    fun `any other page is remembered`() {
        assertNotNull(save(HttpMethod.GET, "/console"), "a page behind the login page is worth resuming")
    }

    @Test
    fun `a form post is not remembered`() {
        assertNull(
            save(HttpMethod.POST, "/consent"),
            "replaying a POST as a browser navigation would drop its body"
        )
    }

    @Test
    fun `an api call is not remembered`() {
        assertNull(
            save(HttpMethod.GET, "/users", accept = MediaType.APPLICATION_JSON),
            "anything answered with a 401 rather than the login page is never replayed"
        )
    }

    /** Runs one request past the cache and reports what it left in the session, if anything. */
    private fun save(
        method: HttpMethod,
        path: String,
        query: String? = null,
        accept: MediaType = MediaType.TEXT_HTML
    ): Any? {
        val request = MockHttpServletRequest(method.name(), path).apply {
            queryString = query
            addHeader("Accept", accept.toString())
        }
        cache.saveRequest(request, MockHttpServletResponse())
        return request.getSession(false)?.getAttribute(SAVED_REQUEST_ATTRIBUTE)
    }

    private companion object {
        /** Where `HttpSessionRequestCache` puts it. */
        const val SAVED_REQUEST_ATTRIBUTE = "SPRING_SECURITY_SAVED_REQUEST"
    }
}
