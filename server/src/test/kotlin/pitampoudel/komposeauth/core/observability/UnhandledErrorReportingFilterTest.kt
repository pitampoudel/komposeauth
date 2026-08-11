package pitampoudel.komposeauth.core.observability

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.sentry.IScope
import io.sentry.ScopeCallback
import io.sentry.Sentry
import io.sentry.protocol.SentryId
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/**
 * The behaviour under test is "the operator finds out". A filter that throws produces a whitelabel
 * 500 with no controller and no `@ControllerAdvice` involved, so unless something reports it on the
 * way past, the only record of a failed sign-in is the visitor's screenshot.
 */
class UnhandledErrorReportingFilterTest {

    private val filter = UnhandledErrorReportingFilter()
    private val captured = mutableListOf<Throwable>()

    @BeforeEach
    fun stubSentry() {
        mockkStatic(Sentry::class)
        every { Sentry.captureException(any<Throwable>(), any<ScopeCallback>()) } answers {
            captured += firstArg<Throwable>()
            SentryId.EMPTY_ID
        }
    }

    @AfterEach
    fun releaseSentry() {
        unmockkStatic(Sentry::class)
        captured.clear()
    }

    private fun run(chain: FilterChain, uri: String = "/login/oauth2/code/google") {
        val request = MockHttpServletRequest("GET", uri).apply {
            // Present on the real callback, and deliberately not something the report may repeat.
            queryString = "code=4/0AVMB&state=z1-tKIgfd"
        }
        filter.doFilter(request, MockHttpServletResponse(), chain)
    }

    @Test
    fun `reports an exception thrown further down the chain`() {
        val boom = IllegalStateException("sign-in filter blew up")

        assertThrows(IllegalStateException::class.java) {
            run(FilterChain { _, _ -> throw boom })
        }

        assertEquals(listOf<Throwable>(boom), captured)
    }

    @Test
    fun `reports errors, not only exceptions`() {
        // A class missing from the deployed image is the failure this filter exists to surface, and
        // it arrives as an Error. Catching Exception alone would let it through unreported.
        val missing = NoClassDefFoundError("com/google/api/client/googleapis/auth/oauth2/GoogleIdTokenVerifier")

        assertThrows(NoClassDefFoundError::class.java) {
            run(FilterChain { _, _ -> throw missing })
        }

        assertEquals(listOf<Throwable>(missing), captured)
    }

    @Test
    fun `keeps the authorization code out of the report`() {
        val scope = slot<ScopeCallback>()
        every { Sentry.captureException(any<Throwable>(), capture(scope)) } returns SentryId.EMPTY_ID

        assertThrows(IllegalStateException::class.java) {
            run(FilterChain { _, _ -> throw IllegalStateException("boom") })
        }

        val tags = mutableMapOf<String, String>()
        val recording = io.mockk.mockk<IScope>(relaxed = true)
        every { recording.setTag(any(), any()) } answers {
            tags[firstArg()] = secondArg()
        }
        scope.captured.run(recording)

        // The path identifies the route; the query string carries the code and state, so it stays out.
        assertEquals("GET /login/oauth2/code/google", tags["route"])
        assertEquals("servlet-filter", tags["origin"])
    }

    @Test
    fun `stays out of the way when nothing fails`() {
        val chain = MockFilterChain()

        run(chain)

        assertEquals(0, captured.size)
    }
}
