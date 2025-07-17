package com.vardansoft.auth.oauth_clients.dto

import org.springframework.security.oauth2.core.AuthorizationGrantType

val GOOGLE_ID_TOKEN_GRANT_TYPE = AuthorizationGrantType("urn:ietf:params:oauth:grant-type:google_id_token")

data class CreateClientRequest(
    val clientId: String? = null,
    val clientName: String,
    val redirectUris: Set<String> = emptySet(),
    val clientUri: String? = null,
    val logoUri: String? = null
)
