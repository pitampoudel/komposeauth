package com.vardansoft.authx.oauth_clients.dto

import kotlinx.serialization.Serializable
import org.springframework.security.oauth2.core.AuthorizationGrantType

val GOOGLE_ID_TOKEN_GRANT_TYPE = AuthorizationGrantType("urn:ietf:params:oauth:grant-type:google_id_token")

@Serializable
data class CreateClientRequest(
    val clientId: String? = null,
    val clientName: String,
    val redirectUris: Set<String> = emptySet(),
    val clientUri: String? = null,
    val logoUri: String? = null
)
