package pitampoudel.komposeauth.core.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.config.isAdmin
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.data.ProfileResponse
import pitampoudel.komposeauth.user.service.mapToProfileResponseDto

/**
 * The root of the server, which two different callers reach for two different reasons.
 *
 * A client asking for JSON wants the signed-in user's profile, and still gets it. A browser wants
 * somewhere to be — and used to get the same JSON, which is where every sign-in that had nothing
 * better to resume ended up: a page of raw profile fields on a host the visitor never chose to
 * visit, with nothing on it to click. The two are told apart by `Accept` rather than by giving one
 * of them a different URL, because `/` is the address a browser lands on by default and the one
 * `SavedRequestAwareAuthenticationSuccessHandler` falls back to.
 */
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
     * Where a signed-in browser is sent instead.
     *
     * An operator gets the console, which is the only thing on this server there is to look at.
     * Anyone else is sent back to the product they signed in for, if its address is configured —
     * this server holds nothing that concerns them, and saying so with a redirect beats saying it
     * with a page nobody would read twice.
     *
     * The login page is the last resort. It is a poor destination for somebody already signed in,
     * but it is a page rather than a payload, and it is reachable without any of the above being
     * set — which is the state a fresh install is in.
     */
    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun landing(): String {
        val user = userContextService.getUserFromAuthentication()
        if (user.isAdmin() || user.roles.contains(Roles.SUPER_ADMIN)) return "redirect:/admin"

        val website = appConfigService.getConfig().websiteUrl?.takeIf { it.isNotBlank() }
        return "redirect:${website ?: "/session-login"}"
    }
}
