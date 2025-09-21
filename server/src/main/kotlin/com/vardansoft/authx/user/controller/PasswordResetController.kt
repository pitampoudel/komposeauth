package com.vardansoft.authx.user.controller

import com.vardansoft.authx.core.service.EmailService
import com.vardansoft.authx.core.service.JwtService
import com.vardansoft.authx.data.UpdateUserRequest
import com.vardansoft.authx.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/reset-password")
class PasswordResetController(
    private val userService: UserService,
    private val emailService: EmailService,
    private val jwtService: JwtService
) {
    @PutMapping
    @ResponseBody
    fun sendResetLink(@RequestParam email: String): ResponseEntity<*> {
        val user = userService.findUserByEmailOrPhone(email)
            ?: return ResponseEntity.badRequest().body("No user with that email")

        val link = jwtService.generateResetPasswordLink(userId = user.id.toHexString())

        emailService.sendSimpleMail(
            to = email,
            subject = "Reset Your Password",
            text = "Click the link to reset your password: $link"
        )

        return ResponseEntity.ok("Reset link sent")
    }

    @PostMapping
    @ResponseBody
    fun resetPassword(
        @RequestParam token: String,
        @RequestParam newPassword: String,
        @RequestParam confirmPassword: String,
    ): ResponseEntity<*> {

        val userId = jwtService.retrieveClaimsIfValidResetPasswordToken(token).subject

        val user = userService.findUser(userId)
            ?: return ResponseEntity.notFound().build<String>()

        userService.updateUser(
            userId = user.id,
            req = UpdateUserRequest(
                password = newPassword,
                confirmPassword = confirmPassword
            )
        )

        return ResponseEntity.ok("Password updated successfully")
    }
}
