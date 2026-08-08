package pitampoudel.komposeauth.core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Honours the OIDC `prompt` parameter on the authorization endpoint.
 *
 * Spring Authorization Server ignores `prompt`, so a relying party that wants the user to sign in
 * as somebody else gets a code re-issued for whoever already owns the session — the user never
 * sees a login page and cannot switch accounts. When `prompt=login` or `prompt=select_account`
 * is present this filter drops the session authentication, so the authorization request falls
 * through to the entry point and the login page is shown again.
 *
 * The authorization request that triggered this is saved and replayed once the user has logged
 * back in, and it still carries `prompt`. Its `state` is therefore remembered in the session so
 * the replay is let through instead of clearing the fresh authentication and looping forever.
 */
class PromptReAuthenticationFilter(
    private val authorizationEndpointUri: String,
    private val securityContextRepository: SecurityContextRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (requiresReAuthentication(request)) {
            request.getSession(false)?.setAttribute(HANDLED_PROMPT_ATTRIBUTE, promptKey(request))
            clearAuthentication(request, response)
        }
        filterChain.doFilter(request, response)
    }

    private fun requiresReAuthentication(request: HttpServletRequest): Boolean {
        if (request.requestURI != request.contextPath + authorizationEndpointUri) return false
        if (!requestsReAuthentication(request.getParameter(PROMPT_PARAMETER))) return false

        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null ||
            !authentication.isAuthenticated ||
            authentication is AnonymousAuthenticationToken
        ) return false

        val session = request.getSession(false) ?: return false
        return session.getAttribute(HANDLED_PROMPT_ATTRIBUTE) != promptKey(request)
    }

    private fun clearAuthentication(request: HttpServletRequest, response: HttpServletResponse) {
        // Saving an empty context removes it from the session, without discarding the session
        // itself — the saved authorization request and the marker above have to survive.
        val emptyContext = SecurityContextHolder.createEmptyContext()
        SecurityContextHolder.setContext(emptyContext)
        securityContextRepository.saveContext(emptyContext, request, response)

        // AnonymousAuthenticationFilter has already run by this point, and ExceptionTranslationFilter
        // only forwards to the login entry point for an authentication it considers anonymous — a
        // null one gets a 403 instead. So stand in for the filter that no longer gets a say.
        val anonymousContext = SecurityContextHolder.createEmptyContext()
        anonymousContext.authentication = AnonymousAuthenticationToken(
            ANONYMOUS_KEY,
            ANONYMOUS_PRINCIPAL,
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        )
        SecurityContextHolder.setContext(anonymousContext)
    }

    /** `state` identifies a single authorization request; the query string is the fallback. */
    private fun promptKey(request: HttpServletRequest): String =
        request.getParameter("state") ?: request.queryString.orEmpty()

    companion object {
        const val PROMPT_PARAMETER = "prompt"

        private const val HANDLED_PROMPT_ATTRIBUTE = "pitampoudel.komposeauth.HANDLED_PROMPT"
        private const val ANONYMOUS_KEY = "komposeauth-prompt-reauthentication"
        private const val ANONYMOUS_PRINCIPAL = "anonymousUser"

        /** `prompt` is a space-delimited list of values (OIDC Core 3.1.2.1). */
        fun requestsReAuthentication(prompt: String?): Boolean =
            prompt?.split(" ")?.any { it == "login" || it == "select_account" } == true
    }
}
