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
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#3458d4")
        if (error != null) {
            model.addAttribute(
                "error",
                when (error) {
                    "locked" ->
                        "This account has been deactivated. Contact support to get it reopened."
                    // Sign-in through Google or Apple got as far as the provider and failed on the
                    // way back, so nothing about the password is worth mentioning — and there is
                    // nothing the visitor can fix by retyping it.
                    "provider" ->
                        "We couldn't finish signing you in with that account. Try again, or use your email and password."
                    else ->
                        "That email or password didn't match. Check them and try again, or reset your password."
                }
            )
        }
        return "session-login"
    }
}
