package pitampoudel.komposeauth.core.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders

/**
 * Whether [origin] names the host this request was addressed to, ignoring scheme and port.
 *
 * Three answers to "what host is this server reached at", because no single one is available
 * everywhere. `serverName` is the right one and is what `ForwardedHeaderFilter` rewrites from
 * `X-Forwarded-Host` — but only where the proxy sends that header and
 * `server.forward-headers-strategy` is left at `framework`. Where it is not, `serverName` is the
 * container's internal name and the request's own `Host` or `X-Forwarded-Host` is all there is to
 * go on. None of the three can be set by a page: `Host` is a forbidden header name, and
 * `X-Forwarded-Host` is not CORS-safelisted, so a scripted request carrying one is preflighted and
 * the preflight does not carry it.
 */
fun isOwnOrigin(origin: String, request: HttpServletRequest): Boolean {
    val originHost = hostOfOrigin(origin) ?: return false
    return ownHostCandidates(request).any { originHost.equals(it, ignoreCase = true) }
}

/**
 * The host named by a serialized origin, or null if the value is not one.
 *
 * Matched against the grammar rather than handed to a URI parser. An `Origin` is not a URI: RFC 6454
 * serializes it as scheme, host and optional port and nothing else — no userinfo, no path, no query,
 * no fragment — and a browser never sends anything else. A general parser accepts all of those and
 * answers with a host anyway, so `//auth.example.com` and `https://auth.example.com/../../x` both
 * came back as this server's own origin. Neither is reachable from a browser, so neither was a way
 * in; matching the real grammar simply means there is nothing to reason about, and no URL-shaped
 * value built out of a request header for a scanner to trip over.
 */
private fun hostOfOrigin(origin: String): String? =
    SERIALIZED_ORIGIN.matchEntire(origin.trim())?.groups?.get("host")?.value

/** scheme "://" host [ ":" port ] — a bracketed IPv6 literal, or a name of letters, digits, dots and hyphens. */
private val SERIALIZED_ORIGIN = Regex(
    """[A-Za-z][A-Za-z0-9+.\-]*://(?<host>\[[0-9A-Fa-f:.]+]|[A-Za-z0-9._\-]+)(?::\d{1,5})?"""
)

/**
 * Whether this request was sent by a page on some other site.
 *
 * A browser attaches `Origin` to every cross-site POST and a page cannot suppress or forge it, so
 * this is enough to turn away a form submitted from somewhere else. A request with no `Origin` at
 * all is not one: browsers always send it here, and the callers that do not are not browsers.
 */
fun isCrossOriginRequest(request: HttpServletRequest): Boolean {
    val origin = request.getHeader(HttpHeaders.ORIGIN)?.takeIf { it.isNotBlank() } ?: return false
    return !isOwnOrigin(origin, request)
}

private fun ownHostCandidates(request: HttpServletRequest): List<String> = listOfNotNull(
    request.serverName,
    hostOf(request.getHeader(HttpHeaders.HOST)),
    // A list when several proxies appended to it; the leftmost is the one the browser addressed.
    hostOf(request.getHeader("X-Forwarded-Host")?.substringBefore(','))
)

/** The host named by an authority such as `Host`, with any port stripped. */
private fun hostOf(authority: String?): String? {
    val host = authority?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    // IPv6 literals are bracketed, so the colon that separates the port is the one after `]`.
    val portSeparator = if (host.startsWith("[")) {
        host.indexOf(':', host.indexOf(']').takeIf { it >= 0 } ?: 0)
    } else {
        host.indexOf(':')
    }
    return if (portSeparator > 0) host.substring(0, portSeparator) else host
}
