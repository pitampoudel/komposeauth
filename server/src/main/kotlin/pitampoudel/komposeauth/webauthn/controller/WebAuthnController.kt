package pitampoudel.komposeauth.webauthn.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.data.LoginOptionsResponse
import pitampoudel.komposeauth.core.domain.ApiEndpoints.LOGIN_OPTIONS
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.webauthn.service.PasskeyService

@RestController
class WebAuthnController(
    private val passkeys: PasskeyService,
    private val userContextService: UserContextService,
    private val appConfigService: AppConfigService
) {
    @Operation(
        summary = "Get configuration for login",
        description = "Returns client configuration, such as the Google Client ID, and passkey sign-in options. " +
                "The options' challenge is what the signed answer is matched back to, so no cookie is needed.",
        tags = ["webauthn"]
    )
    @GetMapping("/$LOGIN_OPTIONS")
    fun getLoginOptions(
        @RequestParam(name = "platform", required = true)
        platform: Platform
    ): ResponseEntity<LoginOptionsResponse> = ResponseEntity.ok(
        LoginOptionsResponse(
            googleClientId = appConfigService.googleClientId(platform),
            publicKeyAuthOptionsJson = passkeys.signInOptions()
        )
    )

    @Operation(
        summary = "Get WebAuthn register options",
        description = "Returns PublicKeyCredentialCreationOptions for adding a passkey to the signed-in account",
        tags = ["webauthn"]
    )
    @PostMapping("/webauthn/register/options")
    fun getRegisterOptions(authentication: Authentication): ResponseEntity<String> =
        ResponseEntity.ok(passkeys.registrationOptions(userContextService.getUserFromAuthentication(authentication)))

    @Operation(
        summary = "Complete WebAuthn registration",
        description = "Verifies the new passkey against the options it answers and adds it to the signed-in account",
        tags = ["webauthn"]
    )
    @PostMapping("/webauthn/register")
    fun finishRegistration(
        authentication: Authentication,
        @RequestBody
        registrationRequest: String
    ): ResponseEntity<MessageResponse> {
        passkeys.register(userContextService.getUserFromAuthentication(authentication), registrationRequest)
        return ResponseEntity.ok(MessageResponse("Passkey added"))
    }
}
