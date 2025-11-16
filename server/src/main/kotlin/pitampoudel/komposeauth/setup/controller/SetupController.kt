package pitampoudel.komposeauth.setup.controller

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import pitampoudel.komposeauth.StaticAppProperties
import pitampoudel.komposeauth.setup.entity.Env
import pitampoudel.komposeauth.setup.service.EnvService
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Controller
class SetupController(
    private val envService: EnvService,
    private val appProps: StaticAppProperties,
) {
    @GetMapping("/setup")
    fun setupForm(model: Model, @RequestParam("key") key: String): String {
        val decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8.toString())
        if (decodedKey != appProps.base64EncryptionKey) {
            throw AccessDeniedException("You are not authorized")
        }
        model.addAttribute("config", envService.getEnv())
        return "setup"
    }

    @PostMapping("/setup")
    fun submit(@RequestParam("key") key: String, @ModelAttribute form: Env, model: Model): String {
        val decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8.toString())
        if (decodedKey != appProps.base64EncryptionKey) {
            throw AccessDeniedException("You are not authorized")
        }
        envService.save(form)
        envService.clearCache()
        model.addAttribute("config", envService.getEnv())
        return "setup"
    }
}