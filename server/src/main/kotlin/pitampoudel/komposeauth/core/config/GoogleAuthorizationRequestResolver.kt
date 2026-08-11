package pitampoudel.komposeauth.core.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

/**
 * Adds `prompt=select_account` (and an optional `login_hint`) to the authorization request we send
 * to Google.
 *
 * Without `prompt`, Google silently reuses the single account it has a session for, so a user who
 * wants to sign in with a different account has no way to get the account chooser.
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

        val parameters = LinkedHashMap<String, Any>(authorizationRequest.additionalParameters)
        parameters["prompt"] = "select_account"
        request.getParameter("login_hint")?.takeIf { it.isNotBlank() }?.let {
            parameters["login_hint"] = it
        }

        // `from` deliberately drops the built URI, so `build` regenerates it with the new params.
        return OAuth2AuthorizationRequest.from(authorizationRequest)
            .additionalParameters(parameters)
            .build()
    }
}
