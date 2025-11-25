package pitampoudel.komposeauth.user.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.web.util.UrlUtils
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.dto.mapToProfileResponseDto
import java.net.URLEncoder

@RestController
class HomeController(
    private val userContextService: UserContextService,
    private val kycService: KycService,
) {

    @GetMapping("/")
    fun home(request: HttpServletRequest): ResponseEntity<*> {
        return runCatching {
            userContextService.getUserFromAuthentication()
        }.map { user ->
            val profile = user.mapToProfileResponseDto(kycService.isVerified(user.id))
            ResponseEntity.ok(profile)
        }.getOrElse {
            val continueUrl = URLEncoder.encode(UrlUtils.buildFullRequestUrl(request), Charsets.UTF_8)
            ResponseEntity.status(302)
                .header("Location", "/login-bridge.html?continue=$continueUrl")
                .build<Any>()
        }
    }
}
