package pitampoudel.komposeauth.core.security.ratelimit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

/**
 * These are the cases that decide whether the abuse limits can be walked past, so they are written
 * from the attacker's side: what happens when the caller sends the header themselves.
 */
class ClientIpResolverTest {

    private fun resolver(trustedProxyCount: Int) = ClientIpResolver(
        RateLimitProperties().apply { this.trustedProxyCount = trustedProxyCount }
    )

    private fun request(peer: String, forwardedFor: String? = null) =
        MockHttpServletRequest().apply {
            remoteAddr = peer
            forwardedFor?.let { addHeader("X-Forwarded-For", it) }
        }

    @Test
    fun `ignores a forwarded header when no proxy is declared`() {
        // The whole point of the default. Directly exposed, the header is just something the caller
        // typed; believing it would let one attacker present as unlimited distinct clients.
        val resolved = resolver(trustedProxyCount = 0)
            .resolve(request(peer = "203.0.113.9", forwardedFor = "1.2.3.4"))

        assertEquals("203.0.113.9", resolved)
    }

    @Test
    fun `takes the address the single trusted proxy observed`() {
        val resolved = resolver(trustedProxyCount = 1)
            .resolve(request(peer = "10.0.0.1", forwardedFor = "198.51.100.7"))

        assertEquals("198.51.100.7", resolved)
    }

    @Test
    fun `ignores entries the client prepended ahead of the trusted proxy`() {
        // The attack: send your own X-Forwarded-For and the proxy appends to it rather than
        // replacing it, so the forged value ends up on the left. Counting from the right steps over
        // however many entries were planted.
        val resolved = resolver(trustedProxyCount = 1).resolve(
            request(peer = "10.0.0.1", forwardedFor = "1.1.1.1, 2.2.2.2, 198.51.100.7")
        )

        assertEquals("198.51.100.7", resolved)
    }

    @Test
    fun `counts in from the right through a chain of two proxies`() {
        val resolved = resolver(trustedProxyCount = 2).resolve(
            request(peer = "10.0.0.2", forwardedFor = "forged, 198.51.100.7, 10.0.0.1")
        )

        assertEquals("198.51.100.7", resolved)
    }

    @Test
    fun `falls back to the peer when the chain is shorter than declared`() {
        // Either a misconfigured hop count or a request that arrived by some other route. Trusting
        // the leftmost entry here is exactly the bug being avoided, so use the peer instead.
        val resolved = resolver(trustedProxyCount = 3)
            .resolve(request(peer = "10.0.0.1", forwardedFor = "1.2.3.4"))

        assertEquals("10.0.0.1", resolved)
    }

    @Test
    fun `falls back to the peer when a proxy is declared but no header arrives`() {
        val resolved = resolver(trustedProxyCount = 1).resolve(request(peer = "10.0.0.1"))

        assertEquals("10.0.0.1", resolved)
    }

    @Test
    fun `resolves a managed platform's client address from the last entry`() {
        // Cloud Run and its equivalents append the caller's address as the final entry, so a service
        // reached at the platform's own URL is one hop — and anything the caller put there itself is
        // to the left of it.
        val resolved = resolver(trustedProxyCount = 1).resolve(
            request(peer = "169.254.8.130", forwardedFor = "attacker-supplied, 198.51.100.7")
        )

        assertEquals("198.51.100.7", resolved)
    }

    @Test
    fun `resolves the client past a load balancer that appends its own address too`() {
        // An external Application Load Balancer appends both the client it saw and its own
        // forwarding rule, which is why that deployment is two hops rather than one.
        val resolved = resolver(trustedProxyCount = 2).resolve(
            request(peer = "169.254.8.130", forwardedFor = "198.51.100.7, 34.117.0.1")
        )

        assertEquals("198.51.100.7", resolved)
    }

    @Test
    fun `still refuses to guess when no proxy is declared on a managed platform`() {
        // The safe answer, not the useful one: every caller collapses onto the platform's own
        // address and shares one budget. The resolver logs a warning naming the setting when it
        // sees this, because the symptom otherwise points nowhere near the cause.
        val resolved = resolver(trustedProxyCount = 0).resolve(
            request(peer = "169.254.8.130", forwardedFor = "198.51.100.7")
        )

        assertEquals("169.254.8.130", resolved)
    }

    @Test
    fun `tolerates the spacing real proxies produce`() {
        val resolved = resolver(trustedProxyCount = 1).resolve(
            request(peer = "10.0.0.1", forwardedFor = "  1.1.1.1 ,   198.51.100.7  ")
        )

        assertEquals("198.51.100.7", resolved)
    }
}
