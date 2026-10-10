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
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.MasterKeyValidator
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.controller.AdminShell
import pitampoudel.komposeauth.core.domain.Roles
import java.util.Locale

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
                members = listOf("supportEmail", "rpId", "defaultPhoneRegion")
            ),
            Group(
                title = "Storage (storageProvider picks where new files go when both buckets are set; old files stay where they are)",
                members = listOf(
                    "storageProvider",
                    "gcpProjectId",
                    "gcpBucketName",
                    "s3BucketName",
                    "s3Region",
                    "s3AccessKeyId",
                    "s3SecretAccessKey"
                )
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
                title = "Rate Limiting",
                members = listOf("trustedProxyCount", "clientIpHeader")
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
                    "sparrowFromNumber",
                    "whatsappAccessToken",
                    "whatsappPhoneNumberId"
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
                    ConfigFieldGroup.ConfigField.SelectOption("sparrow", "Sparrow"),
                    ConfigFieldGroup.ConfigField.SelectOption("whatsapp", "WhatsApp")
                )

                "storageProvider" -> listOf(
                    ConfigFieldGroup.ConfigField.SelectOption("", "Not set"),
                    ConfigFieldGroup.ConfigField.SelectOption(AppConfig.STORAGE_GCS, "Google Cloud Storage"),
                    ConfigFieldGroup.ConfigField.SelectOption(AppConfig.STORAGE_S3, "Amazon S3")
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
                "storageProvider" -> "select"
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
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        noStore(response)
        enforceConfigAccessOrLock(model = model, request = request, response = response)?.let { return it }
        val config = appConfigProvider.get()
        adminShell.apply(model)
        model.addAttribute("config", config)
        model.addAttribute("fieldGroups", fieldGroups(config))
        return "admin/config"
    }

    @PostMapping("/admin/config")
    fun submit(
        @ModelAttribute form: AppConfig,
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String {
        noStore(response)
        enforceConfigAccessOrLock(model = model, request = request, response = response)?.let { return it }
        // Carried in the form so the next save is let in the way this request was.
        model.addAttribute("masterKey", postedKey(request)?.takeIf(masterKeyValidator::isValid))
        if (request.getParameter(UNLOCK_PARAM) != null) {
            val config = appConfigProvider.get()
            adminShell.apply(model)
            model.addAttribute("config", config)
            model.addAttribute("fieldGroups", fieldGroups(config))
            return "admin/config"
        }
        (storageChoiceProblem(form) ?: phoneRegionProblem(form))?.let { problem ->
            adminShell.apply(model)
            model.addAttribute("config", form)
            model.addAttribute("fieldGroups", fieldGroups(form))
            model.addAttribute("error", problem)
            return "admin/config"
        }
        val config = appConfigProvider.save(form)
        adminShell.apply(model)
        model.addAttribute("config", config)
        model.addAttribute("fieldGroups", fieldGroups(config))
        model.addAttribute("saved", true)
        return "admin/config"
    }

    /**
     * Neither cloud is a default, so a form naming two buckets without saying which takes new files
     * is refused here, before every upload starts failing.
     */
    private fun storageChoiceProblem(form: AppConfig): String? {
        val candidate = form.copy().clean()
        val touched = candidate.gcpBucketName != null || candidate.s3BucketName != null || candidate.storageProvider != null
        if (!touched) return null
        return runCatching { candidate.resolvedStorageProvider() }.exceptionOrNull()?.message
    }

    /** A region libphonenumber doesn't know would refuse every number typed without a `+`. */
    private fun phoneRegionProblem(form: AppConfig): String? {
        val region = form.copy().clean().defaultPhoneRegion ?: return null
        return if (region in Locale.getISOCountries()) null
        else "defaultPhoneRegion must be a two-letter country code, such as NP"
    }

    /**
     * This page renders every secret the server holds; keep it out of caches and history.
     *
     * No `Referrer-Policy: no-referrer` here: it makes the browser send `Origin: null` with the
     * page's own form post, which reads as a foreign origin.
     */
    private fun noStore(response: HttpServletResponse) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private")
        response.setHeader("Pragma", "no-cache")
    }

    private fun enforceConfigAccessOrLock(
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): String? {
        // There is deliberately no "no users yet, let anyone in" bootstrap here. This page reads and
        // writes every secret the server holds — SMTP password, Twilio token, Google client secret —
        // so opening it to the internet for the window between deploy and first signup hands a fresh
        // instance to whoever finds it first. The operator already has BASE64_ENCRYPTION_KEY, which
        // is required to boot, so the master key below is always available to them for first-run.
        val suppliedKey = request.getHeader(MASTER_KEY_HEADER) ?: postedKey(request)
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
        if (suppliedKey != null) response.status = HttpServletResponse.SC_FORBIDDEN
        model.addAttribute("appName", appConfigProvider.get().name.orEmpty())
        model.addAttribute("keyRejected", suppliedKey != null)
        model.addAttribute("keyInAddress", request.queryString != null && request.getParameter(KEY_PARAM) != null)
        return "admin/config-locked"
    }

    /**
     * The key typed into this page's own form. Never one from the address, where it lands in
     * access logs, proxy logs, browser history and the Referer of whatever the page loads — and
     * since the servlet API hands back a posted field and a query parameter of the same name
     * alike, a request whose URL carries a query can't supply the key as a parameter at all.
     */
    private fun postedKey(request: HttpServletRequest): String? =
        request.takeIf { it.method == "POST" && it.queryString == null }?.getParameter(KEY_PARAM)

    companion object {
        const val MASTER_KEY_HEADER = "X-Master-Key"
        private const val KEY_PARAM = "key"

        /** Sent by the locked page: show the form for the key it carries, rather than save it. */
        private const val UNLOCK_PARAM = "unlock"
    }
}
