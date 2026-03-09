package pitampoudel.komposeauth.kyc.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.servlet.view.RedirectView
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.domain.ApiEndpoints.THIRD_FACTOR_KYC
import pitampoudel.komposeauth.core.service.jwt.JwtTokenService
import pitampoudel.komposeauth.core.utils.findServerUrl
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.dto.ThirdFactorModel
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.UserService

@RestController
class ThirdFactorKycController(
    val userService: UserService,
    private val kycService: KycService,
    val appConfigService: AppConfigService,
    val jwtTokenService: JwtTokenService,
    val userContextService: UserContextService,
    val restClient: RestClient
) {

    @GetMapping("/$THIRD_FACTOR_KYC")
    fun generateUrl(httpServletRequest: HttpServletRequest): RedirectView {
        val user = userContextService.getUserFromAuthentication()
        val secretKey = appConfigService.getConfig().thirdFactorSecretKey
            ?: error("Third-factor secret key is not configured")
        val token = appConfigService.getConfig().thirdFactorToken
            ?: error("Third-factor token is not configured")
        val thirdFactorUrl = appConfigService.getConfig().thirdFactorUrl?.takeIf { it.isNotBlank() }
            ?: error("Third-factor URL is not configured")
        val jwt = jwtTokenService.generateHs256Token(
            secretKey = secretKey,
            subject = user.id.toHexString(),
            issuer = findServerUrl(httpServletRequest),
            claims = mapOf(
                "name" to user.fullName,
                "token" to token,
                "identifier" to user.id.toHexString(),
                "label" to "",
                "secondary_label" to "",
                "callback" to findServerUrl(httpServletRequest) + "/$THIRD_FACTOR_KYC",
                "is_sdk" to "true"
            )
        )
        val response = restClient.post()
            .uri("$thirdFactorUrl/tfauth/get-kyc-url/")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("jwt" to jwt))
            .retrieve()
            .body<ThirdFactorKycUrlResponse>()
            ?: error("Third-factor KYC URL response was empty")
        return RedirectView(response.url)
    }

    @PostMapping("/$THIRD_FACTOR_KYC")
    fun submit(@Validated @RequestBody data: ThirdFactorModel): ResponseEntity<KycResponse> {
        return ResponseEntity.ok(kycService.submitThirdFactorVerification(data))
    }

    data class ThirdFactorKycUrlResponse(
        val url: String
    )

}
