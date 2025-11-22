package pitampoudel.komposeauth.app_config.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import pitampoudel.komposeauth.StaticAppProperties
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Controller
class AppConfigController(
    private val appConfigService: AppConfigService,
    private val appProps: StaticAppProperties,
) {
    @GetMapping("/setup")
    @Operation(
        summary = "web page to configure this app"
    )
    fun setupForm(model: Model, @RequestParam("key") key: String): String {
        val decodedKey = key.replace(" ", "+")
        if (decodedKey != appProps.base64EncryptionKey) {
            throw AccessDeniedException("You are not authorized. $decodedKey <> ${appProps.base64EncryptionKey}")
        }
        model.addAttribute("config", appConfigService.getEnv())
        return "setup"
    }

    @PostMapping("/setup")
    fun submit(@RequestParam("key") key: String, @ModelAttribute form: AppConfig, model: Model): String {
        val decodedKey = key.replace(" ", "+")
        if (decodedKey != appProps.base64EncryptionKey) {
            throw AccessDeniedException("You are not authorized")
        }
        appConfigService.save(form)
        appConfigService.clearCache()
        model.addAttribute("config", appConfigService.getEnv())
        return "setup"
    }
}