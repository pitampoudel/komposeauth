package pitampoudel.komposeauth.core.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.security.PendingAuthorizationFilter
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
     * Where a sign-in with nothing to resume lands, and only that.
     *
     * A visitor who came from a relying party has something to resume even when the saved request
     * is gone — see [PendingAuthorizationFilter] for how it goes missing, which takes no more than
     * opening a second tab. Telling them the sign-in worked is true and useless: the application
     * that sent them here got nothing, and asking them to "head back" only starts it again. So they
     * are sent back to their own authorization request, which is now a signed-in visitor arriving
     * at the endpoint and answered with a code.
     *
     * Somebody who simply visited this host gets the page, which is what it is for.
     */
    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun landing(request: HttpServletRequest, model: Model): String {
        PendingAuthorizationFilter.take(request)?.let { return "redirect:$it" }

        val user = userContextService.getUserFromAuthentication()
        val config = appConfigService.getConfig()
        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#3458d4")
        model.addAttribute("account", user.email ?: user.phoneNumber ?: "")
        return "signed-in"
    }
}
