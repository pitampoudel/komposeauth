package com.vardansoft.komposeauth.general.controller

import com.vardansoft.komposeauth.core.config.UserContextService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(
    private val userContextService: UserContextService
) {
    @GetMapping("/")
    fun index(model: Model): String {
        val user = userContextService.getUserFromAuthentication()
        model.addAttribute("fullName", user.fullName)
        model.addAttribute("email", user.email)

        return "index"
    }
}
