package com.vardansoft.authx.oauth_clients.dto

import kotlinx.serialization.Serializable


@Serializable
data class CreateClientRequest(
    val clientId: String? = null,
    val clientName: String,
    val redirectUris: Set<String> = emptySet(),
    val clientUri: String? = null,
    val logoUri: String? = null
)
