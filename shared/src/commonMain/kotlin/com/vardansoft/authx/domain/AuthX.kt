package com.vardansoft.authx.domain

import io.ktor.client.plugins.auth.AuthConfig

interface AuthX {
    val authUrl: String
    val clientId: String
    val hosts: List<String>
    fun configureBearer(auth: AuthConfig)
}