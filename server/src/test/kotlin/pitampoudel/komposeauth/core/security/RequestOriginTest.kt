package pitampoudel.komposeauth.core.security

import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Site-wide CSRF protection is off, so this is the whole of what stands between a hostile page and
 * the configuration form — the one endpoint here that binds a plain form and rewrites every secret
 * the server holds.
 */
class RequestOriginTest {

    private fun request(
        serverName: String = "auth.example.com",
        host: String? = "auth.example.com",
        forwardedHost: String? = null,
        origin: String? = null
    ) = MockHttpServletRequest("POST", "/admin/config").apply {
        this.serverName = serverName
        host?.let { addHeader("Host", it) }
        forwardedHost?.let { addHeader("X-Forwarded-Host", it) }
        origin?.let { addHeader("Origin", it) }
    }

    @Test
    fun `a form posted from another site is cross-origin`() {
        assertTrue(isCrossOriginRequest(request(origin = "https://evil.example.com")))
    }

    @Test
    fun `the console's own post is not`() {
        assertFalse(isCrossOriginRequest(request(origin = "https://auth.example.com")))
    }

    @Test
    fun `a different port or scheme on our own host is not`() {
        // Browsers attach Origin to same-origin posts too, and behind a proxy terminating TLS the
        // server can believe it is on http while the page says https. Refusing that would lock the
        // operator out of the page holding the setting that would fix it.
        assertFalse(isCrossOriginRequest(request(origin = "http://auth.example.com:8443")))
    }

    @Test
    fun `the Host header is enough when serverName is the container's own name`() {
        assertFalse(
            isCrossOriginRequest(
                request(serverName = "internal.local", origin = "https://auth.example.com")
            )
        )
    }

    @Test
    fun `X-Forwarded-Host is enough when neither of the others is`() {
        assertFalse(
            isCrossOriginRequest(
                request(
                    serverName = "internal.local",
                    host = "internal.local",
                    forwardedHost = "auth.example.com, inner-proxy.local",
                    origin = "https://auth.example.com"
                )
            )
        )
    }

    @Test
    fun `no Origin at all is not treated as cross-origin`() {
        // A browser always sends it on a cross-site POST and a page cannot suppress it, so a request
        // without one did not come from a browser and is not what this is guarding against.
        assertFalse(isCrossOriginRequest(request(origin = null)))
    }

    @Test
    fun `an unparseable Origin is refused`() {
        assertTrue(isCrossOriginRequest(request(origin = "not a url")))
    }

    /**
     * An `Origin` is not a URI. RFC 6454 serializes it as scheme, host and optional port and nothing
     * else, and a browser never sends anything else — so anything that is not that shape is refused
     * outright rather than having a host pulled out of it. Handing these to a general URI parser
     * answered `auth.example.com` for the first two.
     */
    @Test
    fun `only a serialized origin can name us`() {
        listOf(
            "//auth.example.com",                 // no scheme
            "https://auth.example.com/../../x",   // a path
            "https://auth.example.com#@evil.com", // a fragment
            "https://evil.com@auth.example.com",  // userinfo
            "auth.example.com",                   // a bare host
            "null"                                // sandboxed iframe, data: URL
        ).forEach { origin ->
            assertTrue(
                isCrossOriginRequest(request(origin = origin)),
                "'$origin' is not a serialized origin and must not be taken for ours"
            )
        }
    }

    @Test
    fun `userinfo never lets another host pass as ours`() {
        assertTrue(isCrossOriginRequest(request(origin = "https://auth.example.com@evil.com")))
    }

    @Test
    fun `an IPv6 origin is understood`() {
        assertFalse(
            isCrossOriginRequest(
                request(serverName = "[::1]", host = "[::1]:8080", origin = "http://[::1]:8080")
            )
        )
    }
}
