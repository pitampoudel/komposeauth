package pitampoudel.komposeauth.core.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.savedrequest.HttpSessionRequestCache

/**
 * Sends a signed-in visitor on to whatever brought them here.
 *
 * The saved request in the session is still the first answer, and the usual one. This adds the
 * fallback for when it has gone — see [RelyingPartyReturn] — so that a visitor sent here by an
 * application is returned to it rather than left on this server's own root, which answers JSON.
 *
 * The two records are kept in step: whichever one is used, the other is dropped, so a sign-in that
 * resumed from the session cannot leave a cookie behind to divert an unrelated sign-in later.
 */
open class ReturnToRelyingPartyHandler(
    private val relyingPartyReturn: RelyingPartyReturn
) : SavedRequestAwareAuthenticationSuccessHandler() {

    private val requestCache = HttpSessionRequestCache()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        if (requestCache.getRequest(request, response) != null) {
            relyingPartyReturn.clear(request, response)
        }
        super.onAuthenticationSuccess(request, response, authentication)
    }

    /** Only consulted when there was no saved request, which is the case this exists for. */
    override fun determineTargetUrl(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String = relyingPartyReturn.resume(request, response)
        ?: super.determineTargetUrl(request, response)
}
