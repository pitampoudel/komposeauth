package com.vardansoft.komposeauth.core.controller

import com.vardansoft.komposeauth.AppProperties
import com.vardansoft.komposeauth.domain.Platform
import io.swagger.v3.oas.annotations.Operation // Added import
import kotlinx.serialization.Serializable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/config")
class ConfigController(

    val appProperties: AppProperties
) {
    @Serializable
    data class ConfigResponse(val googleClientId: String)

    @Operation(
        summary = "Get client configuration",
        description = "Returns client configuration, such as the Google Client ID."
    )
    @GetMapping
    fun getConfig(
        @RequestParam(
            name = "platform",
            required = true
        ) platform: Platform
    ): ResponseEntity<ConfigResponse> {
        return ResponseEntity.ok(ConfigResponse(appProperties.googleClientId(platform)))
    }
}
