package pitampoudel.komposeauth.user.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.domain.ApiEndpoints.USERS

@Controller
@RequestMapping("/$USERS/dashboard")
class UsersDashboardPageController(
    private val appConfigService: AppConfigService,
    private val userContextService: UserContextService
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    fun page(model: Model): String {
        val config = appConfigService.getConfig()
        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#3458d4")
        // The page mirrors the server's own rule that only a SUPER_ADMIN may hand out
        // SUPER_ADMIN, so the control is disabled with a reason instead of failing on click.
        model.addAttribute(
            "viewerRoles",
            userContextService.authenticatedUserOrNull()?.roles.orEmpty()
        )
        return "users-dashboard"
    }
}
