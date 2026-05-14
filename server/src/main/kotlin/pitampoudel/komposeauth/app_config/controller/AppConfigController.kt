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
    fun fields(value: AppConfig) = buildFields(
        klass = AppConfig::class,
        value = value,
        excludedFieldNames = setOf("id", "createdAt", "updatedAt"),
        optionsFor = {
            when (it.name) {
                "smsProvider" -> listOf(
                    ConfigField.SelectOption("", "None"),
                    ConfigField.SelectOption("twilio", "Twilio"),
                    ConfigField.SelectOption("samaye", "Samaye"),
                    ConfigField.SelectOption("sparrow", "Sparrow")
                )

                else -> null
            }
        },
        inputTypeFor = { property ->
            return@buildFields when {
                property.returnType.classifier == Int::class -> "number"
                property.javaField?.isAnnotationPresent(URL::class.java) == true -> "url"
                property.javaField?.isAnnotationPresent(Email::class.java) == true -> "email"
                else -> when (property.name) {
                    "corsAllowedOriginList" -> "textarea"
                    "allowedAndroidSha256List" -> "textarea"
                    "smsProvider" -> "select"
                    else -> "text"
                }
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
        model.addAttribute("fields", fields(config))
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
        model.addAttribute("fields", fields(config))
        return "config"
    }
}
