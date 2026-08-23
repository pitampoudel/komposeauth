package pitampoudel.komposeauth.core.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.config.isAdmin
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.data.ProfileResponse
import pitampoudel.komposeauth.user.service.mapToProfileResponseDto


@Controller
class HomeController(
    private val userContextService: UserContextService,
    private val kycService: KycService,
    private val appConfigService: AppConfigService,
) {

    @GetMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun profile(): ResponseEntity<ProfileResponse> {
        val user = userContextService.getUserFromAuthentication()
        return ResponseEntity.ok(user.mapToProfileResponseDto(kycService.isVerified(user.id)))
    }


    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun landing(model: Model): String {
        val user = userContextService.getUserFromAuthentication()
        if (user.isAdmin() || user.roles.contains(Roles.SUPER_ADMIN)) return "redirect:/admin"

        val config = appConfigService.getConfig()
        config.websiteUrl?.takeIf { it.isNotBlank() }?.let { return "redirect:$it" }

        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#3458d4")
        model.addAttribute("account", user.email ?: user.phoneNumber ?: "")
        return "signed-in"
    }
}
