package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import org.apache.coyote.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import jakarta.servlet.http.HttpServletRequest
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.utils.ServerUrl
import pitampoudel.komposeauth.core.domain.ApiEndpoints.RESET_PASSWORD
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService

@Controller
@RequestMapping("/$RESET_PASSWORD")
class PasswordResetController(
    private val userService: UserService,
    private val emailService: EmailService,
    private val oneTimeTokenService: OneTimeTokenService,
    private val appConfigService: AppConfigService,
    private val serverUrl: ServerUrl,
    private val taskExecutor: TaskExecutor
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "Show password reset form",
        description = "Displays a form to reset the password using a token from the email."
    )
    @GetMapping
    fun resetPasswordForm(@RequestParam token: String, model: Model): String {
        // Verify token without consuming
        oneTimeTokenService.findValidToken(token, OneTimeToken.Purpose.RESET_PASSWORD)
        val config = appConfigService.getConfig()
        model.addAttribute("token", token)
        model.addAttribute("appName", config.name?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("logoUrl", config.logoUrl?.takeIf { it.isNotBlank() } ?: "")
        model.addAttribute("brandColor", config.brandColor?.takeIf { it.isNotBlank() } ?: "#4f46e5")
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
        // This endpoint is public, so the answer must not differ for an address that has an
        // account and one that doesn't — otherwise it reports who is registered here.
        // The account may be found by phone number too, so mail goes to the address on the account.
        val user = userService.findByUserName(email)
        val address = user?.email
        if (user != null && address != null) {
            val baseUrl = serverUrl.of(request)
            // Off the request thread, so an address with an account is answered as fast as one
            // without. A failure can't be reported to the caller without giving the account away,
            // so it has to surface in the logs instead of the response.
            taskExecutor.execute {
                try {
                    val link = oneTimeTokenService.generateResetPasswordLink(userId = user.id, baseUrl = baseUrl)
                    val sent = emailService.sendHtmlMail(
                        baseUrl = baseUrl,
                        to = address,
                        subject = "Reset Your Password",
                        template = "email/generic",
                        model = mapOf(
                            "recipientName" to user.firstNameOrUser(),
                            "message" to "Click the button below to reset your password.",
                            "actionUrl" to link,
                            "actionText" to "Reset Password"
                        )
                    )
                    if (!sent) log.error("Failed to send password reset email for user {}", user.id)
                } catch (e: Exception) {
                    log.error("Failed to send password reset email for user {}", user.id, e)
                }
            }
        }

        return ResponseEntity.ok(
            MessageResponse("If that address has an account, a reset link is on its way.")
        )
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
    ): ResponseEntity<MessageResponse> {
        val stored = oneTimeTokenService.findValidToken(token, OneTimeToken.Purpose.RESET_PASSWORD)
        val user = userService.findUser(stored.userId.toHexString())
            ?: throw BadRequestException("User not found")
        // Validate the new password before burning the token, so a typo doesn't cost the user their
        // reset link.
        val update = UpdateProfileRequest(
            password = newPassword,
            confirmPassword = confirmPassword
        )
        // Then consume before applying: the token must not survive to be replayed, and a concurrent
        // second request has to lose the race rather than reset the password twice.
        oneTimeTokenService.consume(token, OneTimeToken.Purpose.RESET_PASSWORD)
        userService.updateUser(
            userId = user.id,
            req = update,
            // The emailed one-time token is the proof of ownership here; the user is resetting the
            // password precisely because they cannot supply the current one.
            requireReauthentication = false
        )
        // Answered in place: the form submits with fetch, which cannot follow a redirect to the
        // website on another origin, and showed an error after the password had already changed.
        return ResponseEntity.ok(MessageResponse("Your password has been reset. You can now sign in."))
    }
}
