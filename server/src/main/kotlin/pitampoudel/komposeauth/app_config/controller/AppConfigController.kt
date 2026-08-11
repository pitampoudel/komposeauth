package pitampoudel.komposeauth.app_config.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.MasterKeyValidator
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.controller.AdminShell
import pitampoudel.komposeauth.core.domain.Roles

@Controller
class AppConfigController(
    private val appConfigProvider: AppConfigProvider,
    private val masterKeyValidator: MasterKeyValidator,
    private val adminShell: AdminShell,
    val userContextService: UserContextService
) {
    fun fieldGroups(value: AppConfig) = buildFieldGroups(
        schema = AppConfig::class,
        value = value,
        excludedFieldNames = setOf("id", "createdAt", "updatedAt"),
        preferredGroups = listOf(
            Group(
                title = "Branding",
                members = listOf("name", "logoUrl", "brandColor", "websiteUrl")
            ),
            Group(
                title = "Social Links",
                members = listOf(
                    "facebookLink",
                    "instagramLink",
                    "tiktokLink",
                    "linkedinLink",
                    "youtubeLink",
                    "privacyLink"
                )
            ),
            Group(
                title = "Support & Platform",
                members = listOf("supportEmail", "rpId", "gcpProjectId", "gcpBucketName")
            ),
            Group(
                title = "OAuth",
                members = listOf(
                    "googleAuthClientId",
                    "googleAuthClientSecret",
                    "googleAuthDesktopClientId",
                    "googleAuthDesktopClientSecret",
                    "appleAuthClientId"
                )
            ),
            Group(
                title = "Security",
                members = listOf("allowedAndroidSha256List", "corsAllowedOriginList")
            ),
            Group(
                title = "Roles",
                members = listOf("rolesCatalog")
            ),
            Group(
                title = "SMS Provider",
                members = listOf(
                    "smsProvider",
                    "twilioAccountSid",
                    "twilioAuthToken",
                    "twilioFromNumber",
                    "twilioVerifyServiceSid",
                    "samayeApiKey",
                    "sparrowApiToken",
                    "sparrowFromNumber"
                )
            ),
            Group(
                title = "SMTP",
                members = listOf(
                    "smtpHost",
                    "smtpPort",
                    "smtpUsername",
                    "smtpPassword",
                    "smtpFromEmail",
                    "smtpFromName",
                    "emailFooterText"
                )
            ),
            Group(
                title = "Monitoring & Alerts",
                members = listOf("sentryDsn", "slackBotToken", "slackChannelId")
            ),
            Group(
                title = "Third-factor KYC",
                members = listOf("thirdFactorUrl", "thirdFactorSecretKey", "thirdFactorToken")
            )
        ),
        optionsFor = {
            when (it.name) {
                "smsProvider" -> listOf(
                    ConfigFieldGroup.ConfigField.SelectOption("", "None"),
                    ConfigFieldGroup.ConfigField.SelectOption("twilio", "Twilio"),
                    ConfigFieldGroup.ConfigField.SelectOption("samaye", "Samaye"),
                    ConfigFieldGroup.ConfigField.SelectOption("sparrow", "Sparrow")
                )

                else -> null
            }
        },
        inputTypeFor = { property ->
            when (property.name) {
                "corsAllowedOriginList" -> "textarea"
                "allowedAndroidSha256List" -> "textarea"
                "rolesCatalog" -> "textarea"
                "smsProvider" -> "select"
                else -> null
            }
        }
    )


    @GetMapping("/admin/config")
    @Operation(
        summary = "web page to configure this app"
    )
    fun form(
        model: Model,
        @RequestParam("key", required = false)
        key: String?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        enforceConfigAccessOrRedirect(key = key, request = request)?.let { return it }
        val config = appConfigProvider.get()
        noStore(response)
        adminShell.apply(model)
        model.addAttribute("config", config)
        model.addAttribute("fieldGroups", fieldGroups(config))
        return "admin/config"
    }

    @PostMapping("/admin/config")
    fun submit(
        @RequestParam("key", required = false) key: String?,
        @ModelAttribute form: AppConfig,
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        enforceConfigAccessOrRedirect(key = key, request = request)?.let { return it }
        val config = appConfigProvider.save(form)
        noStore(response)
        adminShell.apply(model)
        model.addAttribute("config", config)
        model.addAttribute("fieldGroups", fieldGroups(config))
        model.addAttribute("saved", true)
        return "admin/config"
    }

    /** This page renders every secret the server holds; keep it out of caches and history. */
    private fun noStore(response: HttpServletResponse) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private")
        response.setHeader("Pragma", "no-cache")
        response.setHeader("Referrer-Policy", "no-referrer")
    }

    private fun enforceConfigAccessOrRedirect(key: String?, request: HttpServletRequest): String? {
        // There is deliberately no "no users yet, let anyone in" bootstrap here. This page reads and
        // writes every secret the server holds — SMTP password, Twilio token, Google client secret —
        // so opening it to the internet for the window between deploy and first signup hands a fresh
        // instance to whoever finds it first. The operator already has BASE64_ENCRYPTION_KEY, which
        // is required to boot, so the master key below is always available to them for first-run.
        //
        // The key may also arrive as a header, so operators aren't forced to put it in a URL where
        // it lands in access logs, proxy logs and browser history.
        val suppliedKey = key ?: request.getHeader(MASTER_KEY_HEADER)
        if (masterKeyValidator.isValid(suppliedKey)) {
            return null
        }
        val user = userContextService.authenticatedUserOrNull()
        if (user != null) {
            if (!user.roles.any { it == Roles.SUPER_ADMIN }) {
                throw AccessDeniedException("Only super admins can access configuration.")
            }
            return null
        }
        // `/login` is the JSON login API, not a page; the browser sign-in page is /session-login.
        return "redirect:/session-login"
    }

    companion object {
        const val MASTER_KEY_HEADER = "X-Master-Key"
    }
}
