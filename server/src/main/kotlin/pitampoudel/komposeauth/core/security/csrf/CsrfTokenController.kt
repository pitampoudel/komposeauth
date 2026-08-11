package pitampoudel.komposeauth.core.security.csrf

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Hands a browser app the CSRF token it cannot read for itself.
 *
 * An app served from a different registrable domain than this server can never read the CSRF
 * cookie — the same-origin policy forbids it — so without this endpoint such an app has no way to
 * satisfy the protection and every write it makes with the access-token cookie would be refused.
 *
 * Returning the token in a readable body is safe because CORS decides who may read it: the response
 * is only legible to an origin on the configured allow-list, and only when the caller sends
 * credentials. An attacker's page is not on that list, so it can trigger this request but never see
 * the answer — which is the same guarantee the cookie itself provides.
 *
 * Apps on a subdomain of `rpId` do not need this: the cookie is scoped to `.rpId` and is readable
 * directly.
 */
@RestController
class CsrfTokenController {

    data class CsrfTokenResponse(
        val token: String,
        val headerName: String,
        val parameterName: String
    )

    @GetMapping("/csrf")
    @Operation(
        summary = "Fetch a CSRF token",
        description = "Call with credentials before the first state-changing request from a browser " +
                "app that authenticates with the access-token cookie. Echo the token back in the " +
                "returned header on every such request."
    )
    fun token(
        token: CsrfToken?,
        response: HttpServletResponse
    ): ResponseEntity<CsrfTokenResponse> {
        // The token is per-session authority; a shared cache must never be able to hand one
        // browser's token to another.
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private")
        response.setHeader("Pragma", "no-cache")

        // Null only if CSRF protection is switched off entirely, which it is not in any supported
        // configuration; say so plainly rather than returning a token-shaped empty string.
        if (token == null) return ResponseEntity.notFound().build()

        // Reading `.token` is what resolves the deferred token and writes the cookie.
        return ResponseEntity.ok(
            CsrfTokenResponse(
                token = token.token,
                headerName = token.headerName,
                parameterName = token.parameterName
            )
        )
    }
}
