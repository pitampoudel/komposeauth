package pitampoudel.komposeauth.webauthn.controller

import com.webauthn4j.converter.util.ObjectConverter
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.AppProperties
import pitampoudel.komposeauth.data.ApiEndpoints.LOGIN_OPTIONS
import pitampoudel.komposeauth.data.LoginOptionsResponse
import pitampoudel.komposeauth.domain.Platform

@RestController
class WebAuthnController(
    private val objectConverter: ObjectConverter,
    private val rpOperations: WebAuthnRelyingPartyOperations,
    private val requestOptionsRepository: PublicKeyCredentialRequestOptionsRepository,
    private val appProperties: AppProperties
) {
    @Operation(
        summary = "Get configuration for login",
        description = "Returns client configuration, such as the Google Client ID and public key registration options JSON"
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
                googleClientId = appProperties.googleClientId(platform),
                publicKeyAuthOptionsJson = json
            )
        )
    }
}