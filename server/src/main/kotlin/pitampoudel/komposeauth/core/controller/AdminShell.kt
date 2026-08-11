package pitampoudel.komposeauth.core.controller

import org.springframework.stereotype.Component
import org.springframework.ui.Model
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.oauth_clients.repository.OAuth2ClientRepository
import pitampoudel.komposeauth.user.service.UserService

/**
 * Everything `admin/layout.html` needs, in one place.
 *
 * Every admin page is rendered into the same shell, so the branding, the signed-in viewer and the
 * counts the navigation carries are filled in here rather than repeated in each controller.
 */
@Component
class AdminShell(
    private val appConfigService: AppConfigService,
    private val userContextService: UserContextService,
    private val userService: UserService,
    private val oAuth2ClientRepository: OAuth2ClientRepository
) {

    /** Default brand colour, matching the one the sign-in page falls back to. */
    private val defaultBrandColor = "#3458d4"

    fun apply(model: Model) {
        val config = appConfigService.getConfig()
        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: defaultBrandColor)

        model.addAttribute("userCount", userService.countUsers())
        model.addAttribute("clientCount", oAuth2ClientRepository.count())

        // The configuration page is also reachable with a master key before anyone has signed up,
        // so the viewer block is left out entirely rather than rendered empty.
        val viewer = userContextService.authenticatedUserOrNull()
        model.addAttribute(
            "viewerName",
            viewer?.let {
                listOfNotNull(it.firstName, it.lastName).joinToString(" ").takeIf(String::isNotBlank)
                    ?: it.email
                    ?: it.phoneNumber
                    ?: "Signed in"
            }
        )
        model.addAttribute(
            "viewerSub",
            viewer?.roles?.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "No role"
        )
        model.addAttribute("viewerRoles", viewer?.roles.orEmpty())
    }
}
