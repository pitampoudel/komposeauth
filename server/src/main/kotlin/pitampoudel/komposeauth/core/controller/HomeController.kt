package pitampoudel.komposeauth.core.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.data.ProfileResponse
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.dto.mapToProfileResponseDto

@RestController
class HomeController(
    private val userContextService: UserContextService,
    private val kycService: KycService,
) {

    @GetMapping("/")
    fun home(): ResponseEntity<ProfileResponse> {
        val user = userContextService.getUserFromAuthentication()
        val profile = user.mapToProfileResponseDto(kycService.isVerified(user.id))
        return ResponseEntity.ok(profile)
    }
}
