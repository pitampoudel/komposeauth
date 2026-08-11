package pitampoudel.komposeauth.core.security.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Works out which address to hold responsible for a request.
 *
 * This decides whether the abuse limits mean anything at all. `X-Forwarded-For` is a header like any
 * other: anyone who can reach the server can send it, and can send a different one every time. Taken
 * at face value it does not identify a client, it identifies whatever the client felt like typing —
 * so a limiter keyed on it counts nothing and stops nobody.
 *
 * What makes the header trustworthy is position. A reverse proxy appends the address it actually
 * received the connection from, so entries added by proxies you run collect at the *right-hand* end
 * of the list, after anything the client sent. Counting in from that end therefore skips exactly the
 * forged part, however much of it there is. Counting from the left — which is what
 * `ForwardedHeaderFilter` does, and so what `remoteAddr` gives you under
 * `server.forward-headers-strategy: framework` — reads the one entry the attacker fully controls.
 *
 * The count cannot be guessed, only declared: see [RateLimitProperties.trustedProxyCount]. It
 * defaults to 0, which trusts nothing and uses the peer address of the TCP connection — always
 * truthful, and correct for a server exposed directly.
 */
@Component
class ClientIpResolver(private val properties: RateLimitProperties) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val warnedAboutUndeclaredProxy = AtomicBoolean(false)

    fun resolve(request: HttpServletRequest): String {
        val peer = request.remoteAddr?.takeIf { it.isNotBlank() } ?: UNKNOWN

        val hops = properties.trustedProxyCount
        if (hops <= 0) {
            warnIfActuallyProxied(request, peer)
            return peer
        }

        val forwarded = request.getHeader(FORWARDED_FOR)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        // Fewer entries than declared means the chain is not the one described — a request that
        // reached us by some other path, or a misconfigured hop count. Fall back to the peer
        // address, which is at worst the nearest proxy and at best the client, but is never forged.
        if (forwarded.size < hops) return peer

        return forwarded[forwarded.size - hops]
    }

    /**
     * Says so, once, when this looks like a proxied deployment that forgot to declare its proxy.
     *
     * Trusting nothing is the safe default but it is not the harmless one: if a proxy really is in
     * front, every request carries its address instead of the caller's, so one shared bucket holds
     * the whole world and the limits lock all users out together. On a managed platform — Cloud Run
     * and its equivalents — that is the *normal* configuration, not an exotic one, so the mistake is
     * easy to make and the symptom, everybody throttled at once, points nowhere near the cause.
     *
     * The tell is an address that cannot belong to a caller on the internet: loopback, link-local or
     * a private range, arriving on a request that also carries a forwarding header. A directly
     * exposed server sees real public addresses and stays quiet, which is right — there the header
     * is forged and ignoring it is the whole point.
     */
    private fun warnIfActuallyProxied(request: HttpServletRequest, peer: String) {
        if (warnedAboutUndeclaredProxy.get()) return
        if (request.getHeader(FORWARDED_FOR) == null) return
        if (!isInfrastructureAddress(peer)) return
        if (!warnedAboutUndeclaredProxy.compareAndSet(false, true)) return

        log.warn(
            "Abuse limits are counting requests against '{}', which is not a public client address, " +
                    "on requests that carry {}. This server appears to be behind a proxy that has not " +
                    "been declared, so every caller shares one budget and the limits will refuse all " +
                    "of them at once. Set app.rate-limit.trusted-proxy-count (TRUSTED_PROXY_COUNT) to " +
                    "the number of proxies in front of it — 1 for a Cloud Run service reached at its " +
                    "own URL, 2 behind an external Application Load Balancer.",
            peer,
            FORWARDED_FOR
        )
    }

    private fun isInfrastructureAddress(peer: String): Boolean {
        // Only parse things already written as an address; a hostname here would send getByName off
        // to DNS, on the request path.
        if (peer.isEmpty() || peer.any { it !in IP_LITERAL_CHARS }) return false
        return try {
            InetAddress.getByName(peer).run {
                isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress || isAnyLocalAddress
            }
        } catch (_: UnknownHostException) {
            false
        }
    }

    private companion object {
        const val FORWARDED_FOR = "X-Forwarded-For"
        const val UNKNOWN = "unknown"
        val IP_LITERAL_CHARS = ('0'..'9') + ('a'..'f') + ('A'..'F') + listOf('.', ':', '%')
    }
}
