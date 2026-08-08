package pitampoudel.komposeauth.core.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

/**
 * Adds `prompt` (and an optional `login_hint`) to the authorization request we send to Google.
 *
 * Without `prompt`, Google silently reuses the single account it has a session for, so a user who
 * wants to sign in with a different account has no way to get the account chooser. We therefore
 * default to `select_account`, and let a caller override it via `/oauth2/authorization/google?prompt=…`.
 */
class GoogleAuthorizationRequestResolver(
    private val delegate: OAuth2AuthorizationRequestResolver
) : OAuth2AuthorizationRequestResolver {

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        customize(delegate.resolve(request), request)

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String
    ): OAuth2AuthorizationRequest? = customize(delegate.resolve(request, clientRegistrationId), request)

    private fun customize(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest
    ): OAuth2AuthorizationRequest? {
        if (authorizationRequest == null) return null

        val additionalParameters = LinkedHashMap<String, Any>(authorizationRequest.additionalParameters)
        additionalParameters["prompt"] = googlePrompt(request.getParameter("prompt"))
        request.getParameter("login_hint")?.takeIf { it.isNotBlank() }?.let {
            additionalParameters["login_hint"] = it
        }

        // `from` deliberately drops the built URI, so `build` regenerates it with the new params.
        return OAuth2AuthorizationRequest.from(authorizationRequest)
            .additionalParameters(additionalParameters)
            .build()
    }

    companion object {
        /**
         * Google only understands these; anything else (notably OIDC's `login`, which callers of
         * our own authorization endpoint use) is mapped to the default rather than forwarded.
         */
        private val SUPPORTED_PROMPTS = setOf("none", "consent", "select_account")

        private const val DEFAULT_PROMPT = "select_account"

        fun googlePrompt(requested: String?): String {
            val supported = requested.orEmpty().split(" ").filter { it in SUPPORTED_PROMPTS }
            return if (supported.isEmpty()) DEFAULT_PROMPT else supported.joinToString(" ")
        }
    }
}
