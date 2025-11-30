package pitampoudel.komposeauth.app_config.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Controller
class AppConfigController(
    private val appConfigService: AppConfigService
) {
    @GetMapping("/setup")
    @Operation(
        summary = "web page to configure this app"
    )
    @PreAuthorize("@userService.countUsers() == 0 or hasAuthority('ROLE_SUPER_ADMIN') or @masterKeyValidator.isValid(#key)")
    fun setupForm(
        model: Model,
        @RequestParam("key", required = false)
        key: String?,
        auth: Authentication?
    ): String {
        model.addAttribute("config", appConfigService.getEnv())
        return "setup"
    }

    @PostMapping("/setup")
    @PreAuthorize("@userService.countUsers() == 0 or hasAuthority('ROLE_SUPER_ADMIN') or @masterKeyValidator.isValid(#key)")
    fun submit(
        @RequestParam("key", required = false) key: String?,
        @ModelAttribute form: AppConfig,
        model: Model
    ): String {
        appConfigService.save(form)
        appConfigService.clearCache()
        model.addAttribute("config", appConfigService.getEnv())
        return "setup"
    }
}