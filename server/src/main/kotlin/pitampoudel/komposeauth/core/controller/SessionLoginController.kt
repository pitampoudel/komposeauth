package pitampoudel.komposeauth.core.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.util.UriComponentsBuilder
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Controller
class SessionLoginController(private val appConfigService: AppConfigService) {

    /** Reads the authorization request the entry point saved before redirecting us here. */
    private val requestCache = HttpSessionRequestCache()

    @GetMapping("/session-login")
    fun loginPage(
        @RequestParam(required = false) error: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model
    ): String {
        val config = appConfigService.getConfig()
        val googleEnabled =
            !config.googleAuthClientId.isNullOrBlank() && !config.googleAuthClientSecret.isNullOrBlank()

        // `error` means we just came back from a failed or cancelled Google login. Falling through
        // to the form is what stops us bouncing the user straight back to the provider forever.
        if (error == null && googleEnabled) {
            googleRedirect(request, response)?.let { return it }
        }

        model.addAttribute("googleEnabled", googleEnabled)
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

    /**
     * `idp=google` on the authorization request means the relying party wants the user sent
     * straight to Google, Keycloak-style, so the login form is never shown. Null when no such
     * hint was given and the form should be rendered as usual.
     */
    private fun googleRedirect(request: HttpServletRequest, response: HttpServletResponse): String? {
        val saved = requestCache.getRequest(request, response) ?: return null
        fun param(name: String) = saved.getParameterValues(name)?.firstOrNull()?.takeIf { it.isNotBlank() }

        if (!param("idp").equals("google", ignoreCase = true)) return null

        val uri = UriComponentsBuilder.fromPath("/oauth2/authorization/google")
            .apply { param("login_hint")?.let { queryParam("login_hint", it) } }
            .encode()
            .toUriString()
        return "redirect:$uri"
    }
}
