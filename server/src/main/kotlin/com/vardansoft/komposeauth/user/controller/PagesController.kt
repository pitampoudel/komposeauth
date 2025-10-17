package com.vardansoft.komposeauth.user.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PagesController {

    @GetMapping("/signup")
    fun create(): String {
        return "signup"
    }

    @GetMapping("/login")
    fun login(): String {
        return "login"
    }
}