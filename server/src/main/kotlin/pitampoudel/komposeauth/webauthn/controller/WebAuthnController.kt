package pitampoudel.komposeauth.webauthn.controller

import com.webauthn4j.converter.util.ObjectConverter
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.data.ApiEndpoints.LOGIN_OPTIONS
import pitampoudel.komposeauth.data.LoginOptionsResponse
import pitampoudel.komposeauth.domain.Platform

@RestController
class WebAuthnController(
    private val objectConverter: ObjectConverter,
    private val rpOperations: WebAuthnRelyingPartyOperations,
    private val requestOptionsRepository: PublicKeyCredentialRequestOptionsRepository,
    private val publicKeyCredentialCreationOptionsRepository: PublicKeyCredentialCreationOptionsRepository,
    private val appConfigProvider: AppConfigProvider
) {
    @Operation(
        summary = "Get configuration for login",
        description = "Returns client configuration, such as the Google Client ID and public key registration options JSON",
        tags = ["webauthn"]
    )
    @GetMapping("/$LOGIN_OPTIONS")
    fun getLoginOptions(
        @RequestParam(name = "platform", required = true)
        platform: Platform,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<LoginOptionsResponse> {
        val credentialRequestOptions = rpOperations.createCredentialRequestOptions(
            ImmutablePublicKeyCredentialRequestOptionsRequest(null)
        )
        requestOptionsRepository.save(request, response, credentialRequestOptions)
        val json = objectConverter.jsonConverter.writeValueAsString(credentialRequestOptions)

        return ResponseEntity.ok(
            LoginOptionsResponse(
                googleClientId = appConfigProvider.googleClientId(platform),
                publicKeyAuthOptionsJson = json
            )
        )
    }

    @Operation(
        summary = "Get WebAuthn register options",
        description = "Returns PublicKeyCredentialCreationOptions for WebAuthn registration",
        tags = ["webauthn"]
    )
    @PostMapping("/webauthn/register/options")
    fun getRegisterOptions(
        authentication: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        val options = rpOperations.createPublicKeyCredentialCreationOptions(
            ImmutablePublicKeyCredentialCreationOptionsRequest(authentication)
        )
        publicKeyCredentialCreationOptionsRepository.save(request, response, options)
        val json = objectConverter.jsonConverter.writeValueAsString(options)
        return ResponseEntity.ok(json)
    }
}