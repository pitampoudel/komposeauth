package pitampoudel.komposeauth.setup.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import pitampoudel.komposeauth.setup.entity.AppConfig
import pitampoudel.komposeauth.setup.service.AppConfigService

@Controller
@PreAuthorize("hasRole('ADMIN') or @setupAccess.isOpen()")
class SetupController(private val appConfigService: AppConfigService) {
    @GetMapping("/setup")
    fun setupForm(model: Model): String {
        model.addAttribute("config", appConfigService.get())
        return "setup"
    }

    @PostMapping("/setup")
    fun submit(@ModelAttribute form: AppConfig, model: Model): String {
        appConfigService.save(form)
        model.addAttribute("config", appConfigService.get())
        return "setup"
    }
}