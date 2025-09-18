package com.vardansoft.authx.core.controller

import com.vardansoft.authx.AppProperties
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
    data class ConfigResponse(val googleClientId: String)

    @GetMapping
    fun getConfig(@RequestParam(name = "desktop", required = false, defaultValue = "false") desktop: Boolean): ResponseEntity<ConfigResponse> {
        val clientId = if (desktop) appProperties.googleAuthDesktopClientId else googleClientId
        return ResponseEntity.ok(ConfigResponse(clientId))
    }
}
