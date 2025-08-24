package com.vardansoft.authx.core.converters

import com.vardansoft.authx.core.providers.GoogleIdTokenAuthToken
import com.vardansoft.authx.oauth_clients.dto.GOOGLE_ID_TOKEN_GRANT_TYPE
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.authentication.AuthenticationConverter

class GoogleIdTokenGrantAuthConverter : AuthenticationConverter {

    override fun convert(request: HttpServletRequest): Authentication? {
        val parameters = request.parameterMap.toMutableMap()

        val grantType = AuthorizationGrantType(parameters[OAuth2ParameterNames.GRANT_TYPE]?.first())

        if (grantType != GOOGLE_ID_TOKEN_GRANT_TYPE) {
            return null
        }


        // client_id (REQUIRED
        val clientId = parameters[OAuth2ParameterNames.CLIENT_ID]?.first()
        if (parameters[OAuth2ParameterNames.CLIENT_ID]?.size != 1 || clientId == null) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)
        }

        val idToken = parameters[OAuth2ParameterNames.CODE]?.first()
        if (idToken == null) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT)
        }

        parameters.remove(OAuth2ParameterNames.CLIENT_ID)
        parameters.remove(OAuth2ParameterNames.CODE)
        parameters.remove(OAuth2ParameterNames.GRANT_TYPE)

        return GoogleIdTokenAuthToken(
            idToken = idToken,
            clientId = clientId,
            clientPrincipal = SecurityContextHolder.getContext().authentication,
            authorizationGrantType = grantType,
            additionalParameters = parameters
        )
    }

}