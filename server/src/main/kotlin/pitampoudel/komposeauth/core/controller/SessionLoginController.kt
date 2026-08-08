package pitampoudel.komposeauth.core.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.security.web.savedrequest.RequestCache
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.util.UriComponentsBuilder
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Controller
class SessionLoginController(private val appConfigService: AppConfigService) {

    /** Reads the authorization request the entry point saved before redirecting us here. */
    private val requestCache: RequestCache = HttpSessionRequestCache()

    @GetMapping("/session-login")
    fun loginPage(
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) idp: String?,
        @RequestParam(required = false) prompt: String?,
        @RequestParam(name = LOGIN_HINT_PARAM, required = false) loginHint: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model
    ): String {
        val config = appConfigService.getConfig()
        val googleEnabled =
            !config.googleAuthClientId.isNullOrBlank() && !config.googleAuthClientSecret.isNullOrBlank()

        val savedRequest = requestCache.getRequest(request, response)
        fun saved(name: String) =
            savedRequest?.getParameterValues(name)?.firstOrNull()?.takeIf { it.isNotBlank() }

        val requestedIdp = idp?.takeIf { it.isNotBlank() }
            ?: saved(IDP_PARAM)
            ?: saved(IDP_HINT_PARAM)

        // `error` means we just came back from a failed or cancelled Google login. Falling through
        // to the form is what stops us bouncing the user straight back to the provider forever.
        if (error == null && googleEnabled && requestedIdp.equals(GOOGLE, ignoreCase = true)) {
            val uri = googleAuthorizationUrl(prompt = prompt?.takeIf { it.isNotBlank() } ?: saved(PROMPT_PARAM),
                loginHint = loginHint?.takeIf { it.isNotBlank() } ?: saved(LOGIN_HINT_PARAM))

            return "redirect:$uri"
        }

        model.addAttribute("googleEnabled", googleEnabled)
        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#4f46e5")
        model.addAttribute("googleUrl", googleAuthorizationUrl(prompt ?: saved(PROMPT_PARAM)))
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.")
        }
        return "session-login"
    }

    private fun googleAuthorizationUrl(prompt: String?, loginHint: String? = null): String =
        UriComponentsBuilder.fromPath("/oauth2/authorization/$GOOGLE")
            .apply {
                prompt?.takeIf { it.isNotBlank() }?.let { queryParam(PROMPT_PARAM, it) }
                loginHint?.takeIf { it.isNotBlank() }?.let { queryParam(LOGIN_HINT_PARAM, it) }
            }
            .encode()
            .toUriString()

    companion object {
        private const val GOOGLE = "google"

        /** Set by the relying party to send the user straight to a provider, Keycloak-style. */
        const val IDP_PARAM = "idp"
        const val IDP_HINT_PARAM = "idp_hint"
        const val PROMPT_PARAM = "prompt"
        const val LOGIN_HINT_PARAM = "login_hint"
    }
}
