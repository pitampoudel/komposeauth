package pitampoudel.komposeauth.core.providers

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.authentication.AuthenticationConverter

/**
 * Authenticates a public client by its `client_id` alone, which Spring only does for a PKCE code
 * exchange. Here it also covers refreshing, so a browser or mobile app can keep its session.
 *
 * Two things it must not do. It must not claim a request that carries a credential: that belongs to
 * Spring's own converters, and taking it here would ignore the secret and authenticate the client as
 * public. And it must not serve `client_credentials`, which has no user behind it: a client id is
 * public, so that would let anyone who reads it out of an app mint service tokens.
 */
class OAuth2PublicClientAuthConverter : AuthenticationConverter {
    override fun convert(request: HttpServletRequest): Authentication? {
        val clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID)
        if (clientId.isNullOrEmpty()) return null

        val carriesCredential = request.getHeader(HttpHeaders.AUTHORIZATION) != null ||
            request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) != null ||
            request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION) != null
        if (carriesCredential) return null

        val grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE)
        if (grantType == AuthorizationGrantType.CLIENT_CREDENTIALS.value) return null

        return OAuth2PublicClientAuthToken(clientId)
    }
}
