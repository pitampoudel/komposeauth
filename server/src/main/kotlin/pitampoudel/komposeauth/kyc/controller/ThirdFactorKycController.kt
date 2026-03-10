package pitampoudel.komposeauth.kyc.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.apache.coyote.BadRequestException
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.domain.ApiEndpoints.THIRD_FACTOR_KYC
import pitampoudel.komposeauth.core.service.jwt.JwtTokenService
import pitampoudel.komposeauth.core.utils.findServerUrl
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.dto.ThirdFactorModel
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.UserService

@RestController
class ThirdFactorKycController(
    val userService: UserService,
    private val kycService: KycService,
    val appConfigService: AppConfigService,
    val jwtTokenService: JwtTokenService,
    val userContextService: UserContextService,
    val kycRepo: KycVerificationRepository,
    val restClient: RestClient
) {
    @Operation(
        summary = "Generate third-factor KYC URL",
        description = "Generates a third-factor KYC URL for the currently authenticated user."
    )
    @GetMapping("/$THIRD_FACTOR_KYC")
    fun generateUrl(httpServletRequest: HttpServletRequest): ThirdFactorKycUrlResponse {
        val user = userContextService.getUserFromAuthentication()
        val secretKey = appConfigService.getConfig().thirdFactorSecretKey
            ?: error("Third-factor secret key is not configured")
        val token = appConfigService.getConfig().thirdFactorToken
            ?: error("Third-factor token is not configured")
        val thirdFactorUrl = appConfigService.getConfig().thirdFactorUrl
            ?: error("Third-factor URL is not configured")
        val existingKyc = kycRepo.findByUserId(user.id) ?: throw BadRequestException("KYC not found")

        if (existingKyc.status in KycResponse.Status.submitted()) {
            throw BadRequestException("KYC already submitted; cannot resubmit")
        }
        val generatedJwt = jwtTokenService.generateHs256Token(
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
            .body(mapOf("jwt_token" to generatedJwt))
            .retrieve()
            .body<ThirdFactorKycUrlResponse>()
            ?: error("Third-factor KYC URL response was empty")
        return response
    }

    @PostMapping("/$THIRD_FACTOR_KYC")
    fun submit(
        httpServletRequest: HttpServletRequest,
        @RequestBody data: ThirdFactorModel
    ): ResponseEntity<KycResponse> {
        val secretKey = appConfigService.getConfig().thirdFactorSecretKey
            ?: error("Third-factor secret key is not configured")
        jwtTokenService.verifyHs256Token(data.jwt, secretKey)
        return ResponseEntity.ok(kycService.submitThirdFactorVerification(findServerUrl(httpServletRequest), data))
    }

    data class ThirdFactorKycUrlResponse(
        val url: String
    )

}
