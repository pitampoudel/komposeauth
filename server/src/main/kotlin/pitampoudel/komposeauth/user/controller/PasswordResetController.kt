package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import org.apache.coyote.BadRequestException
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.servlet.view.RedirectView
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.utils.findServerUrl
import pitampoudel.komposeauth.core.domain.ApiEndpoints.RESET_PASSWORD
import pitampoudel.komposeauth.core.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService

@Controller
@RequestMapping("/$RESET_PASSWORD")
class PasswordResetController(
    private val userService: UserService,
    private val emailService: EmailService,
    private val oneTimeTokenService: OneTimeTokenService,
    private val appConfigProvider: AppConfigProvider
) {
    @Operation(
        summary = "Show password reset form",
        description = "Displays a form to reset the password using a token from the email."
    )
    @GetMapping
    fun resetPasswordForm(@RequestParam token: String, model: Model): String {
        // Verify token without consuming
        oneTimeTokenService.findValidToken(token, OneTimeToken.Purpose.RESET_PASSWORD)
        model.addAttribute("token", token)
        model.addAttribute("logoUrl", appConfigProvider.logoUrl)
        return "reset-password-form"
    }

    @Operation(
        summary = "Send password reset link",
        description = "Sends a password reset link to the user's email address."
    )
    @PutMapping
    fun sendResetLink(
        @RequestParam email: String,
        request: HttpServletRequest
    ): ResponseEntity<MessageResponse> {
        val user = userService.findByUserName(email)
            ?: return ResponseEntity.badRequest().body(MessageResponse("No user with that email"))

        val link = oneTimeTokenService.generateResetPasswordLink(
            userId = user.id,
            baseUrl = findServerUrl(request)
        )

        val sent = emailService.sendHtmlMail(
            baseUrl = findServerUrl(request),
            to = email,
            subject = "Reset Your Password",
            template = "email/generic",
            model = mapOf(
                "recipientName" to user.firstName,
                "message" to "Click the button below to reset your password.",
                "actionUrl" to link,
                "actionText" to "Reset Password"
            )
        )
        if (!sent) throw Exception("Unable to send password reset email")

        return ResponseEntity.ok(MessageResponse("Reset link sent"))
    }

    @Operation(
        summary = "Reset password",
        description = "Resets the user's password using a token received via email."
    )
    @PostMapping
    fun resetPassword(
        @RequestParam token: String,
        @RequestParam newPassword: String,
        @RequestParam confirmPassword: String
    ): RedirectView {
        val stored = oneTimeTokenService.findValidToken(token, OneTimeToken.Purpose.RESET_PASSWORD)
        val user = userService.findUser(stored.userId.toHexString())
            ?: throw BadRequestException("User not found")
        userService.updateUser(
            userId = user.id,
            req = UpdateProfileRequest(
                password = newPassword,
                confirmPassword = confirmPassword
            )
        )
        oneTimeTokenService.consume(token, OneTimeToken.Purpose.RESET_PASSWORD)
        return RedirectView("${appConfigProvider.websiteUrl}?passwordReset=true")
    }
}
