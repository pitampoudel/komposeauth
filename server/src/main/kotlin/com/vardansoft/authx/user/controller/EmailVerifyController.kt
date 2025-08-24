package com.vardansoft.authx.user.controller

import com.vardansoft.authx.core.config.UserContextService
import com.vardansoft.authx.core.service.EmailService
import com.vardansoft.authx.core.service.JwtService
import com.vardansoft.authx.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/verify-email")
class EmailVerifyController(
    private val jwtService: JwtService,
    private val emailService: EmailService,
    private val userService: UserService,
    val userContextService: UserContextService,
) {

    @PostMapping
    fun sendVerificationEmail(): ResponseEntity<*> {
        val user = userContextService.getCurrentUser()
        val link = jwtService.generateEmailVerificationLink(userId = user.id.toHexString())

        emailService.sendSimpleMail(
            to = user.email,
            subject = "Verify Your Email",
            text = "Click the link to verify your email: $link"
        )

        return ResponseEntity.ok("Verification link sent to your email")
    }

    @GetMapping
    fun verifyEmail(@RequestParam("token") token: String): ResponseEntity<*> {
        val claims = jwtService.retrieveClaimsIfValidEmailVerificationToken(token)

        val user = userService.findUser(claims.subject) ?: return ResponseEntity.notFound().build<String>()

        if (user.emailVerified) {
            return ResponseEntity.ok("Email already verified")
        }

        userService.emailVerified(user.id)

        return ResponseEntity.ok("Email successfully verified")
    }
}
