package pitampoudel.komposeauth.core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UriComponentsBuilder

/**
 * Remembers the authorization request a visitor was sent to the login page from, so a sign-in that
 * loses it can still be carried back to the relying party.
 *
 * The saved request is the only thing that returns a visitor to the application that sent them
 * here, and it is a single slot in the session that any unauthenticated request overwrites. So a
 * visitor who touches anything behind authentication between being sent to the login page and
 * signing in — the root, a bookmark, a second tab — loses the authorization request, and their
 * sign-in ends wherever that other request pointed. Which is `/` more often than not: a page saying
 * the sign-in worked, shown to somebody whose relying party never heard about it and will ask them
 * to sign in all over again.
 *
 * A slot of its own, that nothing else writes to, is what makes that recoverable. [HomeController]
 * reads it and resumes the authorization request rather than rendering a dead end.
 *
 * Written when the endpoint sends the visitor to the login page, and dropped on any other outcome,
 * which is the whole of what "still in flight" means here. A request that ends at the relying party
 * — with a code or with an error — is finished, and somebody who reaches the root afterwards has
 * arrived of their own accord and should be answered as such.
 */
class PendingAuthorizationFilter(private val loginPage: String) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!isAuthorizationRequest(request)) {
            filterChain.doFilter(request, response)
            return
        }

        filterChain.doFilter(request, response)

        // No session means nothing was saved and nothing is waiting: an authorization request that
        // never got as far as needing one has nothing to resume.
        val session = request.getSession(false) ?: return
        if (divertedToLogin(response)) {
            session.setAttribute(ATTRIBUTE, authorizationUri(request))
        } else {
            session.removeAttribute(ATTRIBUTE)
        }
    }

    /**
     * Only the browser's own navigation to the endpoint. A `POST` — the consent form — is not a
     * request that can be replayed by sending somebody's browser back to a URL.
     */
    private fun isAuthorizationRequest(request: HttpServletRequest): Boolean =
        request.method == "GET" &&
                request.requestURI == request.contextPath + PromptReAuthenticationFilter.AUTHORIZATION_ENDPOINT

    private fun divertedToLogin(response: HttpServletResponse): Boolean {
        if (response.status !in 300..399) return false
        val location = response.getHeader(HttpHeaders.LOCATION) ?: return false
        val path = runCatching {
            UriComponentsBuilder.fromUriString(location).build().path
        }.getOrNull()
        return path == loginPage
    }

    /** The request as the visitor's browser would ask for it again, and nothing of their choosing. */
    private fun authorizationUri(request: HttpServletRequest): String {
        val query = request.queryString
        return request.requestURI + if (query.isNullOrBlank()) "" else "?$query"
    }

    companion object {
        private const val ATTRIBUTE = "pitampoudel.komposeauth.PENDING_AUTHORIZATION"

        /**
         * The authorization request this session is still in the middle of, and taken rather than
         * read: whoever asks is about to send the visitor there, and if that trip fails there is
         * nothing to be gained by sending them again.
         */
        fun take(request: HttpServletRequest): String? {
            val session = request.getSession(false) ?: return null
            val pending = session.getAttribute(ATTRIBUTE) as? String ?: return null
            session.removeAttribute(ATTRIBUTE)
            // Written by this filter from the endpoint's own path, so this can only fail if
            // something else put it there. A `//host` value would be a redirect off this server.
            return pending.takeIf { it.startsWith("/") && !it.startsWith("//") }
        }
    }
}
