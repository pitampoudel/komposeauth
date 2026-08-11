package pitampoudel.komposeauth.core.security

import org.springframework.http.HttpMethod
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.ApiEndpoints.THIRD_FACTOR_KYC


object PublicEndpoints {

    /**
     * A public path, and the method it is public for.
     *
     * [method] is null wherever the whole path is open. Naming one matters where a path serves both
     * an anonymous visitor and a signed-in one: `/verify-email` is reached by clicking a link in an
     * email, which carries no credentials, but the same path is also how a signed-in user *asks* for
     * that email — and "public" there does not merely relax the authorization check, it stops
     * authentication being attempted at all (see [purelyPublicRequestMatcher] and its use by the
     * bearer token resolver), so the caller would arrive anonymous and the handler could not tell
     * who to write to.
     */
    data class PublicPath(val pattern: String, val method: HttpMethod? = null)

    /** Fully public paths: authentication is never attempted; an invalid token is ignored. */
    val purelyPublicPaths: List<PublicPath> = listOf(
        PublicPath("/css/**"),
        PublicPath("/js/**"),
        PublicPath("/img/**"),
        PublicPath("/lib/**"),
        PublicPath("/favicon.ico"),
        PublicPath("/assets/**"),
        PublicPath("/session-login"),
        PublicPath("/oauth2/jwks"),
        // Handing out a CSRF token requires no authority, and a browser app needs one before it can
        // make its first authenticated write.
        PublicPath("/csrf"),
        PublicPath("/${ApiEndpoints.LOGIN}"),
        PublicPath("/${ApiEndpoints.LOGOUT}"),
        PublicPath("/signup"),
        PublicPath("/${ApiEndpoints.LOGIN_OPTIONS}"),
        // GET only: following the emailed link. Requesting the email is a POST to the same path and
        // has to know who is asking.
        PublicPath("/${ApiEndpoints.VERIFY_EMAIL}", HttpMethod.GET),
        PublicPath("/${ApiEndpoints.RESET_PASSWORD}"),
        PublicPath("/reset-password"),
        PublicPath("/countries.json"),
        PublicPath("/.well-known/**"),
        PublicPath("/setup"),
        PublicPath("/$THIRD_FACTOR_KYC", HttpMethod.POST)
    )

    /** Public paths that use optional authentication: a supplied token is still validated. */
    val optionalAuthPatterns: List<String> = listOf(
        // The configuration page also accepts a master key, so the filter chain has to let it
        // through to the controller's own access check.
        "/admin/config",
        "/${ApiEndpoints.SEND_OTP}",
        "/users"
    )

    fun purelyPublicRequestMatcher(): RequestMatcher =
        OrRequestMatcher(purelyPublicPaths.map(::matcherFor))

    /**
     * Paths that stay public but must still carry a CSRF token, because a forged call to them has a
     * real effect on a signed-in victim.
     */
    private val csrfProtectedPublicPatterns: Set<String> = setOf(
        // Establishes the session. Without a token an attacker can silently sign a victim into an
        // account the attacker controls, and then read back whatever the victim does in it.
        "/session-login",
        // Ends it. Forgeable logout is only a nuisance, but it is a nuisance an attacker can inflict
        // repeatedly, and nothing needs the exemption: the console sends the token, and native
        // clients sign out with an `Authorization` header, which is exempt on its own account.
        "/${ApiEndpoints.LOGOUT}"
    )

    /**
     * Endpoints exempt from CSRF protection.
     *
     * These deliberately ignore ambient credentials — the bearer token resolver returns null for
     * them — so a forged cross-site request carries no authority and there is nothing to protect.
     *
     * `/login` stays exempt so that native and first-run clients can sign in without first fetching
     * a token, and is not the login-CSRF hole that would suggest: it reads its credentials with
     * `@RequestBody`, so it accepts JSON only. A cross-site HTML form cannot produce that content
     * type — forms may only send form-encoded, multipart or plain text — and a scripted request that
     * sets it triggers a CORS preflight, which fails for any origin not on the allow-list. This is
     * asserted in `CsrfProtectionIntegrationTest`, since it is a property of how `/login` parses its
     * body and would otherwise be silently lost if that ever changed.
     */
    fun csrfExemptRequestMatcher(): RequestMatcher = OrRequestMatcher(
        purelyPublicPaths
            .filterNot { it.pattern in csrfProtectedPublicPatterns }
            .map(::matcherFor)
    )

    private fun matcherFor(path: PublicPath): RequestMatcher {
        val builder = PathPatternRequestMatcher.withDefaults()
        return if (path.method == null) {
            builder.matcher(path.pattern)
        } else {
            builder.matcher(path.method, path.pattern)
        }
    }
}
