package pitampoudel.komposeauth.oauth_clients.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Controller
@RequestMapping("/oauth2/clients/dashboard")
class Oauth2ClientsPageController(private val appConfigService: AppConfigService) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun page(model: Model): String {
        val config = appConfigService.getConfig()
        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#4f46e5")
        return "oauth2-clients"
    }
}
