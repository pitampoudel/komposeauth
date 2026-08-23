package pitampoudel.komposeauth.core.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
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

        // Somebody with a console has somewhere to be, so send them there. The page below tells the
        // visitor there is nothing here and to go back to the app they came from, which is true of
        // everyone else and wrong for exactly these two roles. The pair named here is the pair
        // AdminPageController admits, so the redirect cannot land on a 403.
        if (user.roles.any { it == Roles.ADMIN || it == Roles.SUPER_ADMIN }) {
            return "redirect:/admin"
        }

        val config = appConfigService.getConfig()
        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#3458d4")
        model.addAttribute("account", user.email ?: user.phoneNumber ?: "")
        return "signed-in"
    }
}
