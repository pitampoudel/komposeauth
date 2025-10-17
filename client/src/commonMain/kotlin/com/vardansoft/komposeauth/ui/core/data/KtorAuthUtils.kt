package com.vardansoft.komposeauth.ui.core.data

import com.vardansoft.komposeauth.ui.core.domain.KtorBearerHandler
import io.ktor.client.plugins.auth.AuthConfig
import org.koin.core.scope.Scope

fun Scope.setupBearerAuth(config: AuthConfig) {
    val ktorBearerHandler = get<KtorBearerHandler>()
    ktorBearerHandler.configure(config)
}