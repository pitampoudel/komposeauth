package pitampoudel.komposeauth.core.security.csrf

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService

/**
 * The cookie's attributes are the fix, so they are what is asserted here. A CSRF cookie that a
 * browser app cannot reach is indistinguishable from having no protection to offer it: every write
 * is refused and there is no token to be had.
 */
class CrossOriginCsrfTokenRepositoryTest {

    private fun repository(rpId: String?) = CrossOriginCsrfTokenRepository(
        mockk<AppConfigService> { every { rpId() } returns rpId }
    )

    private fun save(rpId: String?, secure: Boolean): Cookie {
        val repo = repository(rpId)
        val request = MockHttpServletRequest().apply { isSecure = secure }
        val response = MockHttpServletResponse()
        repo.saveToken(repo.generateToken(request), request, response)
        return response.getCookie(CrossOriginCsrfTokenRepository.COOKIE_NAME)
            ?: error("no CSRF cookie written")
    }

    @Test
    fun `is scoped to the configured domain so sibling origins can read it`() {
        // Matches the access-token cookie's reach. Without this the cookie is host-only, and an app
        // on another subdomain can neither read the token nor have it sent.
        assertEquals(".example.com", save(rpId = "example.com", secure = true).domain)
    }

    @Test
    fun `omits the domain entirely when no relying party is configured`() {
        // Regression: `"." + rpId()` used to render the literal `.null`, a domain every browser
        // rejects — so the cookie was dropped and nothing worked at all.
        assertNull(save(rpId = null, secure = true).domain, "expected a host-only cookie")
    }

    @Test
    fun `travels cross-site over https, like the access token it guards`() {
        val cookie = save(rpId = "example.com", secure = true)

        assertEquals("None", cookie.getAttribute("SameSite"))
        assertTrue(cookie.secure, "SameSite=None is only honoured on a Secure cookie")
    }

    @Test
    fun `falls back to lax over plain http, where SameSite=None would be ignored`() {
        val cookie = save(rpId = "example.com", secure = false)

        assertEquals("Lax", cookie.getAttribute("SameSite"))
        assertFalse(cookie.secure)
    }

    @Test
    fun `stays readable to the browser app that must echo it back`() {
        assertFalse(
            save(rpId = "example.com", secure = true).isHttpOnly,
            "the app has to read this one; it is not a credential on its own"
        )
    }

    @Test
    fun `salvages a relying party id written as a url`() {
        // Free text on a config form. Pasting the address is the obvious thing to do.
        assertEquals(".example.com", save(rpId = "https://example.com/", secure = true).domain)
    }

    @Test
    fun `salvages a relying party id written with the leading dot of a cookie domain`() {
        // `"." + rpId` made this `..example.com`, which ResponseCookie rejects outright.
        assertEquals(".example.com", save(rpId = ".example.com", secure = true).domain)
    }

    @Test
    fun `falls back to host-only rather than throwing on a domain it cannot use`() {
        // The failure being prevented: ResponseCookie validates the domain and throws, and because
        // cookies are written inside a filter that exception escapes every handler and turns each
        // affected page into a whitelabel 500 — including the config page holding the typo.
        listOf("not a domain", "-example.com", "example..com", "@@@").forEach { bad ->
            assertNull(
                save(rpId = bad, secure = true).domain,
                "expected a host-only cookie for rpId '$bad' rather than a thrown request"
            )
        }
    }

    @Test
    fun `issues a different token every time`() {
        val repo = repository("example.com")
        val request = MockHttpServletRequest()

        assertNotEquals(repo.generateToken(request).token, repo.generateToken(request).token)
    }

    @Test
    fun `reads back the token it wrote`() {
        val repo = repository("example.com")
        val request = MockHttpServletRequest()
        val token = repo.generateToken(request)
        repo.saveToken(token, request, MockHttpServletResponse())

        val next = MockHttpServletRequest().apply {
            setCookies(Cookie(CrossOriginCsrfTokenRepository.COOKIE_NAME, token.token))
        }

        assertEquals(token.token, repo.loadToken(next)?.token)
    }

    @Test
    fun `reports no token once one has been cleared in the same request`() {
        val repo = repository("example.com")
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie(CrossOriginCsrfTokenRepository.COOKIE_NAME, "stale"))
        }

        repo.saveToken(null, request, MockHttpServletResponse())

        // Otherwise the framework would hand back the value it just deleted instead of minting a
        // fresh one.
        assertNull(repo.loadToken(request))
    }
}
