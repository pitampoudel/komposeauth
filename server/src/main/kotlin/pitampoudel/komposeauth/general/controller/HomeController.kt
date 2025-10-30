package pitampoudel.komposeauth.general.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import pitampoudel.komposeauth.core.config.UserContextService

@Controller
class HomeController(
    private val userContextService: UserContextService
) {
    @GetMapping("/{page:(?:login|register-passkey|signup)}")
    fun page(@PathVariable page: String): String {
        return page
    }

    @GetMapping("/")
    fun index(model: Model): String {
        val user = userContextService.getUserFromAuthentication()
        model.addAttribute("fullName", user.fullName)
        model.addAttribute("email", user.email)

        return "index"
    }
}
