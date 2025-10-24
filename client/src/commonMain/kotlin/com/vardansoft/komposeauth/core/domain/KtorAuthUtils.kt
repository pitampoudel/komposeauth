package com.vardansoft.komposeauth.core.domain

import io.ktor.client.plugins.auth.AuthConfig
import org.koin.core.scope.Scope

fun Scope.setupBearerAuth(config: AuthConfig) {
    val ktorBearerHandler = get<KtorBearerHandler>()
    ktorBearerHandler.configure(config)
}