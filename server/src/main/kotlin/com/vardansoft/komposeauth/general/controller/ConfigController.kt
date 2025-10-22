package com.vardansoft.komposeauth.general.controller

import com.vardansoft.komposeauth.AppProperties
import com.vardansoft.komposeauth.data.LoginConfigResponse
import com.vardansoft.komposeauth.domain.Platform
import com.webauthn4j.converter.util.ObjectConverter
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class ConfigController(
    private val appProperties: AppProperties,
    private val objectConverter: ObjectConverter,
    private val rpOperations: WebAuthnRelyingPartyOperations,
    private val requestOptionsRepository: PublicKeyCredentialRequestOptionsRepository
) {

    @Operation(
        summary = "Get configuration for login",
        description = "Returns client configuration, such as the Google Client ID and public key registration options JSON"
    )
    @GetMapping("/config/login")
    fun getConfig(
        @RequestParam(name = "platform", required = true)
        platform: Platform,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<LoginConfigResponse> {
        val credentialRequestOptions = rpOperations.createCredentialRequestOptions(
            ImmutablePublicKeyCredentialRequestOptionsRequest(null)
        )
        requestOptionsRepository.save(request, response, credentialRequestOptions)
        val json = objectConverter.jsonConverter.writeValueAsString(credentialRequestOptions)

        return ResponseEntity.ok(
            LoginConfigResponse(
                googleClientId = appProperties.googleClientId(platform),
                publicKeyAuthOptionsJson = json
            )
        )
    }

    @GetMapping("/.well-known/assetlinks.json")
    fun getAssetLinks(): ResponseEntity<String> {
        val assetLinksJson = appProperties.assetLinksJson
        if (assetLinksJson.isNullOrBlank()) {
            return ResponseEntity.notFound().build()
        }
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return ResponseEntity(assetLinksJson, headers, org.springframework.http.HttpStatus.OK)
    }
}
