package com.vardansoft.komposeauth.ui.core.domain

import io.ktor.client.plugins.auth.AuthConfig

internal interface KtorBearerHandler {
    val authUrl: String
    val serverUrls: List<String>
    fun configure(auth: AuthConfig)
}