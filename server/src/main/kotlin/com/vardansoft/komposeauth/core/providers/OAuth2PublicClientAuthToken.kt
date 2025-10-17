package com.vardansoft.komposeauth.core.providers

import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken


class OAuth2PublicClientAuthToken(
    val clientId: String
) : OAuth2ClientAuthenticationToken(
    clientId,
    ClientAuthenticationMethod.NONE,
    null,
    null
)