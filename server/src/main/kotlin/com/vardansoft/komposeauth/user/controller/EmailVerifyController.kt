package com.vardansoft.komposeauth.user.controller

import com.vardansoft.komposeauth.core.config.UserContextService
import com.vardansoft.komposeauth.core.service.EmailService
import com.vardansoft.komposeauth.core.service.JwtService
import com.vardansoft.komposeauth.user.service.UserService
import com.vardansoft.core.data.MessageResponse
import io.swagger.v3.oas.annotations.Operation
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

    @Operation(
        summary = "Send verification email",
        description = "Sends an email with a verification link to the currently authenticated user's email address."
    )
    @PostMapping
    fun sendVerificationEmail(): ResponseEntity<MessageResponse> {
        val user = userContextService.getCurrentUser()

        // Check if user email exists
        if (user.email == null) {
            return ResponseEntity.badRequest().body(MessageResponse("User email is not set."))
        }

        val link = jwtService.generateEmailVerificationLink(userId = user.id.toHexString())

        emailService.sendSimpleMail(
            to = user.email,
            subject = "Verify Your Email",
            text = "Click the link to verify your email: $link"
        )

        return ResponseEntity.ok(MessageResponse("Verification link sent to your email"))
    }

    @Operation(
        summary = "Verify email address",
        description = "Verifies the user's email address using the provided token."
    )
    @GetMapping
    fun verifyEmail(@RequestParam("token") token: String): ResponseEntity<MessageResponse> {
        val claims = jwtService.retrieveClaimsIfValidEmailVerificationToken(token)

        val user =
            userService.findUser(claims.subject) ?: return ResponseEntity.notFound().build()

        if (user.emailVerified) {
            return ResponseEntity.ok(MessageResponse("Email already verified"))
        }

        userService.emailVerified(user.id)

        return ResponseEntity.ok(MessageResponse("Email successfully verified"))
    }
}
