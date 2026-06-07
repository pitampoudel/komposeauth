package pitampoudel.komposeauth.core.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Controller
class SessionLoginController(private val appConfigService: AppConfigService) {

    @GetMapping("/session-login")
    fun loginPage(
        @RequestParam(required = false) error: String?,
        model: Model
    ): String {
        val config = appConfigService.getConfig()
        model.addAttribute(
            "googleEnabled",
            !config.googleAuthClientId.isNullOrBlank() && !config.googleAuthClientSecret.isNullOrBlank()
        )
        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#4f46e5")
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.")
        }
        return "session-login"
    }
}
