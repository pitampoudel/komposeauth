package com.vardansoft.authx.core.controller

import com.vardansoft.authx.AppProperties
import io.swagger.v3.oas.annotations.Operation // Added import
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/config")
class ConfigController(
    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val googleClientId: String,
    val appProperties: AppProperties
) {
    @Serializable
    data class ConfigResponse(val googleClientId: String)

    @Operation(
        summary = "Get client configuration",
        description = "Returns client configuration, such as the Google Client ID. The `pkce` parameter can be used to request a client ID for desktop, browser applications."
    )
    @GetMapping
    fun getConfig(
        @RequestParam(
            name = "pkce",
            required = false,
            defaultValue = "false"
        ) pkce: Boolean
    ): ResponseEntity<ConfigResponse> {
        val clientId = if (pkce) appProperties.googleAuthPublicClientId else googleClientId
        return ResponseEntity.ok(ConfigResponse(clientId))
    }
}
