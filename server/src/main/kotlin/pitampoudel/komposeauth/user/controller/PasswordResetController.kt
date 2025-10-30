package pitampoudel.komposeauth.user.controller

import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.JwtService
import pitampoudel.komposeauth.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.service.UserService
import pitampoudel.core.data.MessageResponse
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import pitampoudel.komposeauth.data.ApiEndpoints.RESET_PASSWORD

@Controller
@RequestMapping("/$RESET_PASSWORD")
class PasswordResetController(
    private val userService: UserService,
    private val emailService: EmailService,
    private val jwtService: JwtService
) {
    @Operation(
        summary = "Show password reset form",
        description = "Displays a form to reset the password using a token from the email."
    )
    @GetMapping
    fun resetPasswordForm(@RequestParam token: String, model: Model): String {
        model.addAttribute("token", token)
        return "reset-password-form"
    }

    @Operation(
        summary = "Send password reset link",
        description = "Sends a password reset link to the user's email address."
    )
    @PutMapping
    @ResponseBody
    fun sendResetLink(@RequestParam email: String): ResponseEntity<MessageResponse> {
        val user = userService.findByUserName(email)
            ?: return ResponseEntity.badRequest().body(MessageResponse("No user with that email"))

        val link = jwtService.generateResetPasswordLink(userId = user.id.toHexString())

        val sent = emailService.sendSimpleMail(
            to = email,
            subject = "Reset Your Password",
            text = "Click the link to reset your password: $link"
        )
        if (!sent) throw Exception("Unable to send password reset email")

        return ResponseEntity.ok(MessageResponse("Reset link sent"))
    }

    @Operation(
        summary = "Reset password",
        description = "Resets the user's password using a token received via email."
    )
    @PostMapping
    @ResponseBody
    fun resetPassword(
        @RequestParam token: String,
        @RequestParam newPassword: String,
        @RequestParam confirmPassword: String,
    ): ResponseEntity<MessageResponse> {

        val userId = jwtService.retrieveClaimsIfValidResetPasswordToken(token).subject

        val user = userService.findUser(userId)
            ?: return ResponseEntity.notFound().build()

        userService.updateUser(
            userId = user.id,
            req = UpdateProfileRequest(
                password = newPassword,
                confirmPassword = confirmPassword
            )
        )

        return ResponseEntity.ok(MessageResponse("Password updated successfully"))
    }
}
