package pitampoudel.komposeauth.core.security.ratelimit

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Shows what this server actually receives, so the abuse limits can be configured from observation
 * rather than from a hosting provider's documentation.
 *
 * That documentation is not always enough. Providers disagree with themselves about whether their
 * edge strips `X-Forwarded-For`, how many entries it adds, and which header carries the connecting
 * address; putting a CDN in front changes the answer again. Guessing is not a small mistake — trust
 * the wrong header and callers simply nominate who gets counted, so the limits stop working while
 * still appearing to be on.
 *
 * Call this from a phone on mobile data, somewhere the address is unmistakably yours and not your
 * network's, and set `app.rate-limit.client-ip-header` to whichever header came back holding it.
 * Admin-only: it reveals the shape of the request chain, which is not secret but is nobody else's
 * business.
 */
@RestController
class ClientIpDiagnosticController(
    private val clientIpResolver: ClientIpResolver,
    private val properties: RateLimitProperties
) {

    data class ClientIpDiagnosis(
        /** The address the abuse limits are counting this request against, as configured today. */
        val countedAs: String,
        /** How that was arrived at. */
        val decidedBy: String,
        /** The peer address of the connection itself, which no caller can forge. */
        val connectionPeer: String,
        /** Every header that might carry a client address, exactly as received. */
        val received: Map<String, String>,
        val hint: String
    )

    @GetMapping("/admin/client-ip")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Show how this request's client address was determined",
        description = "Use when configuring abuse limits on a new host: call it from a network whose " +
                "public address you know, and see which header actually carries it."
    )
    fun diagnose(request: HttpServletRequest): ClientIpDiagnosis {
        val configuredHeader = properties.clientIpHeader?.takeIf { it.isNotBlank() }

        val received = CANDIDATE_HEADERS
            .mapNotNull { name -> request.getHeader(name)?.let { name to it } }
            .toMap()

        val decidedBy = when {
            configuredHeader != null && request.getHeader(configuredHeader) != null ->
                "client-ip-header '$configuredHeader'"

            configuredHeader != null ->
                "client-ip-header '$configuredHeader' is configured but absent from this request"

            properties.trustedProxyCount > 0 ->
                "entry ${properties.trustedProxyCount} from the right of X-Forwarded-For"

            else -> "the connection's peer address; no proxy is declared"
        }

        return ClientIpDiagnosis(
            countedAs = clientIpResolver.resolve(request),
            decidedBy = decidedBy,
            connectionPeer = request.remoteAddr.orEmpty(),
            received = received,
            hint = "If 'countedAs' is not the public address you called from, pick the header under " +
                    "'received' that does hold it and set CLIENT_IP_HEADER to its name. If none does, " +
                    "and X-Forwarded-For ends with your address, set TRUSTED_PROXY_COUNT to its " +
                    "position counting from the right instead."
        )
    }

    private companion object {
        /** Names that hosting edges and CDNs commonly use; absent ones are simply omitted. */
        val CANDIDATE_HEADERS = listOf(
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Envoy-External-Address",
            "CF-Connecting-IP",
            "Fly-Client-IP",
            "True-Client-IP",
            "X-Client-IP",
            "Forwarded"
        )
    }
}
