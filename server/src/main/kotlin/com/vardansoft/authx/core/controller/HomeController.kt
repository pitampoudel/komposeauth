package com.vardansoft.authx.core.controller

import com.vardansoft.authx.user.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.security.core.context.SecurityContextHolder

@Controller
class HomeController(
    private val userService: UserService
) {
    @GetMapping("/")
    fun index(model: Model): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = authentication?.name
        if (!userId.isNullOrBlank()) {
            val user = userService.findUser(userId)
            if (user != null) {
                model.addAttribute("fullName", user.fullName)
                model.addAttribute("email", user.email)
            }
        }
        return "index"
    }
}
