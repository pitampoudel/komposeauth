package pitampoudel.komposeauth.core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationTrustResolverImpl
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Honours the OIDC `prompt` parameter on the authorization endpoint.
 *
 * Spring Authorization Server ignores `prompt`, so a relying party that wants the user to sign in
 * as somebody else gets a code re-issued for whoever already owns the session — the user never
 * sees a login page and cannot switch accounts. When `prompt=login` or `prompt=select_account` is
 * present this filter drops the session authentication, saves the authorization request and sends
 * the user to the login page; the saved request is replayed once they have signed back in.
 *
 * That replay still carries `prompt`, so the request's `state` is remembered in the session and
 * the replay is let through instead of clearing the fresh authentication and looping forever.
 */
class PromptReAuthenticationFilter(
    private val securityContextRepository: SecurityContextRepository,
    private val loginEntryPoint: AuthenticationEntryPoint
) : OncePerRequestFilter() {

    private val requestCache = HttpSessionRequestCache()
    private val trustResolver = AuthenticationTrustResolverImpl()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!requiresReAuthentication(request)) {
            filterChain.doFilter(request, response)
            return
        }

        request.session.setAttribute(HANDLED_PROMPT_ATTRIBUTE, promptKey(request))

        // Emptying the context removes it from the session without discarding the session itself —
        // the saved authorization request and the marker above have to survive.
        val emptyContext = SecurityContextHolder.createEmptyContext()
        SecurityContextHolder.setContext(emptyContext)
        securityContextRepository.saveContext(emptyContext, request, response)

        requestCache.saveRequest(request, response)
        loginEntryPoint.commence(
            request,
            response,
            InsufficientAuthenticationException("prompt=${request.getParameter(PROMPT_PARAMETER)}")
        )
    }

    private fun requiresReAuthentication(request: HttpServletRequest): Boolean {
        if (request.requestURI != request.contextPath + AUTHORIZATION_ENDPOINT) return false

        // `prompt` is a space-delimited list of values (OIDC Core 3.1.2.1). `none` is deliberately
        // left alone: Spring Authorization Server already implements it per spec.
        val prompt = request.getParameter(PROMPT_PARAMETER)?.split(" ").orEmpty()
        if (prompt.none { it == "login" || it == "select_account" }) return false

        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null ||
            !authentication.isAuthenticated ||
            trustResolver.isAnonymous(authentication)
        ) return false

        val session = request.getSession(false) ?: return false
        return session.getAttribute(HANDLED_PROMPT_ATTRIBUTE) != promptKey(request)
    }

    /** `state` identifies a single authorization request; the query string is the fallback. */
    private fun promptKey(request: HttpServletRequest): String =
        request.getParameter("state") ?: request.queryString.orEmpty()

    companion object {
        const val AUTHORIZATION_ENDPOINT = "/oauth2/authorize"

        private const val PROMPT_PARAMETER = "prompt"
        private const val HANDLED_PROMPT_ATTRIBUTE = "pitampoudel.komposeauth.HANDLED_PROMPT"
    }
}
