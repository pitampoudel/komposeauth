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


    /**
     * Where a signed-in visitor lands when nothing else claimed them.
     *
     * The fallback used to be `/session-login`, which is a loop with no way out and no error to
     * explain it. `SavedRequestAwareAuthenticationSuccessHandler` sends a successful sign-in here
     * whenever the session holds no saved request -- a bookmarked login page, a stale tab, a
     * visitor who reached `/session-login` any way other than through `/oauth2/authorize` -- so a
     * correct password redirected to `/`, `/` redirected back to the login page, and the form
     * rendered again. Submitting it once more did exactly the same thing. Nothing failed, so
     * nothing was ever said; it simply looked like signing in did not work.
     *
     * So the fallback is a page rather than a redirect. It is a dead end either way -- there is
     * genuinely nowhere else to send them -- but it is one that says so.
     */
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
