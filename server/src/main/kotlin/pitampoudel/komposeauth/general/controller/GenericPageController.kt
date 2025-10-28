package pitampoudel.komposeauth.general.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class GenericPageController {

    @GetMapping("/{page:(?:login|register-passkey|signup)}")
    fun page(@PathVariable page: String): String {
        return page
    }
}