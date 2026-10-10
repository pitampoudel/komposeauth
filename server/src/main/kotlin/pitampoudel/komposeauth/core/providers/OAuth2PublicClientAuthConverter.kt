package pitampoudel.komposeauth.core.providers

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.authentication.AuthenticationConverter

/**
 * Authenticates a public client by its `client_id` alone when it refreshes, so a browser or mobile
 * app can keep its session. Spring authenticates a public client only for the code exchange.
 *
 * It claims nothing but a refresh. The code exchange is left to Spring's own converter, which
 * insists on the PKCE `code_verifier`: taken here, a code would be redeemed without it. It must not
 * claim a request that carries a credential either: that belongs to Spring's own converters, and
 * taking it here would ignore the secret and authenticate the client as public.
 */
class OAuth2PublicClientAuthConverter : AuthenticationConverter {
    override fun convert(request: HttpServletRequest): Authentication? {
        val grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE)
        if (grantType != AuthorizationGrantType.REFRESH_TOKEN.value) return null

        val clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID)
        if (clientId.isNullOrEmpty()) return null

        val carriesCredential = request.getHeader(HttpHeaders.AUTHORIZATION) != null ||
            request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) != null ||
            request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION) != null
        if (carriesCredential) return null

        return OAuth2PublicClientAuthToken(clientId)
    }
}
