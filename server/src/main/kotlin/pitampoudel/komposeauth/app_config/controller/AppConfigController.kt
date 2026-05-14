package pitampoudel.komposeauth.app_config.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.Email
import org.hibernate.validator.constraints.URL
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import kotlin.reflect.jvm.javaField

@Controller
class AppConfigController(
    private val appConfigProvider: AppConfigProvider
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
                "smsProvider" -> "select"
                else -> null
            }
        }
    )


    @GetMapping("/config")
    @Operation(
        summary = "web page to configure this app"
    )
    @PreAuthorize("@userService.countUsers() == 0 or hasAuthority('ROLE_SUPER_ADMIN') or @masterKeyValidator.isValid(#key)")
    fun setupForm(
        model: Model,
        @RequestParam("key", required = false)
        key: String?,
    ): String {
        val config = appConfigProvider.get()
        model.addAttribute("config", config)
        model.addAttribute("fieldGroups", fieldGroups(config))
        return "config"
    }

    @PostMapping("/config")
    @PreAuthorize("@userService.countUsers() == 0 or hasAuthority('ROLE_SUPER_ADMIN') or @masterKeyValidator.isValid(#key)")
    fun submit(
        @RequestParam("key", required = false) key: String?,
        @ModelAttribute form: AppConfig,
        model: Model
    ): String {
        val config = appConfigProvider.save(form)
        model.addAttribute("config", config)
        model.addAttribute("fieldGroups", fieldGroups(config))
        return "config"
    }
}
