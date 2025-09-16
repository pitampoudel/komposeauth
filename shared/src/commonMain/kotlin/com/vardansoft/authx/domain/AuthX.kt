package com.vardansoft.authx.domain

import io.ktor.client.plugins.auth.AuthConfig

interface AuthX {
    val authUrl: String
    val serverUrls: List<String>
    fun configureBearer(auth: AuthConfig)
}