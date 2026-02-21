package pitampoudel.komposeauth.core.providers

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.authentication.AuthenticationConverter

class OAuth2PublicClientAuthConverter : AuthenticationConverter {
    override fun convert(request: HttpServletRequest): Authentication? {
        val parameters = request.parameterMap.toMutableMap()

        val clientId = parameters[OAuth2ParameterNames.CLIENT_ID]?.first()
        if (clientId.isNullOrEmpty()) {
            return null
        }

        return OAuth2PublicClientAuthToken(clientId)
    }
}