package com.vardansoft.auth.core.converters

import com.vardansoft.auth.core.providers.OAuth2PublicClientAuthToken
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.authentication.AuthenticationConverter

class OAuth2PublicClientAuthConverter : AuthenticationConverter {
    override fun convert(request: HttpServletRequest): Authentication? {
        val parameters = request.parameterMap.toMutableMap()

        // client_id (REQUIRED)
        val clientId = parameters[OAuth2ParameterNames.CLIENT_ID]?.first()
        if (clientId.isNullOrEmpty()) {
            throw OAuth2AuthenticationException(
                OAuth2ErrorCodes.INVALID_REQUEST,
            )
        }

        return OAuth2PublicClientAuthToken(clientId)
    }
}
