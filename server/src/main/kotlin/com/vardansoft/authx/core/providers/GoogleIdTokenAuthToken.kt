package com.vardansoft.authx.core.providers

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken


class GoogleIdTokenAuthToken(
    val idToken: String,
    val clientId: String,
    authorizationGrantType: AuthorizationGrantType,
    clientPrincipal: Authentication,
    additionalParameters: Map<String, Any?>
) : OAuth2AuthorizationGrantAuthenticationToken(
    authorizationGrantType,
    clientPrincipal,
    additionalParameters
)