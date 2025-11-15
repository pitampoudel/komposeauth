package pitampoudel.komposeauth.setup.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import pitampoudel.komposeauth.setup.entity.Env
import pitampoudel.komposeauth.setup.service.EnvService

@Controller
@PreAuthorize("hasRole('SUPER_ADMIN') or @setupAccess.isOpen()")
class SetupController(private val envService: EnvService) {
    @GetMapping("/setup")
    fun setupForm(model: Model): String {
        model.addAttribute("config", envService.getEnv())
        return "setup"
    }

    @PostMapping("/setup")
    fun submit(@ModelAttribute form: Env, model: Model): String {
        envService.save(form)
        envService.clearCache()
        model.addAttribute("config", envService.getEnv())
        return "setup"
    }
}