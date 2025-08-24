package com.vardansoft.authx.core.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/config")
class ConfigController(
    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val googleClientId: String
) {
    data class ConfigResponse(val googleClientId: String)

    @GetMapping
    fun getConfig(): ResponseEntity<ConfigResponse> {
        return ResponseEntity.ok(ConfigResponse(googleClientId))
    }
}
