package pitampoudel.komposeauth.core.security.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

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

    fun resolve(request: HttpServletRequest): String {
        val peer = request.remoteAddr?.takeIf { it.isNotBlank() } ?: UNKNOWN

        val hops = properties.trustedProxyCount
        if (hops <= 0) return peer

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

    private companion object {
        const val FORWARDED_FOR = "X-Forwarded-For"
        const val UNKNOWN = "unknown"
    }
}
