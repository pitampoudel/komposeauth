package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.apache.coyote.BadRequestException
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.view.RedirectView
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.data.ApiEndpoints.VERIFY_EMAIL
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.utils.findCurrentBaseUrl
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService

@Controller
@RequestMapping("/$VERIFY_EMAIL")
class EmailVerifyController(
    private val oneTimeTokenService: OneTimeTokenService,
    private val emailService: EmailService,
    private val userService: UserService,
    val userContextService: UserContextService,
    val appConfigProvider: AppConfigProvider
) {

    @Operation(
        summary = "Send verification email",
        description = "Sends an email with a verification link to the currently authenticated user's email address."
    )
    @PostMapping
    fun sendVerificationEmail(request: HttpServletRequest): ResponseEntity<MessageResponse> {
        val user = userContextService.getUserFromAuthentication()

        // Check if user email exists
        if (user.email == null) {
            return ResponseEntity.badRequest().body(MessageResponse("User email is not set."))
        }

        val link = oneTimeTokenService.generateEmailVerificationLink(
            userId = user.id,
            baseUrl = findCurrentBaseUrl(request)
        )

        val sent = emailService.sendHtmlMail(
            to = user.email,
            subject = "Verify Your Email",
            template = "email/generic",
            model = mapOf(
                "name" to user.firstName,
                "title" to "Verify your email",
                "message" to "Click the button below to verify your email address.",
                "actionUrl" to link,
                "actionText" to "Verify Email"
            )
        )
        if (!sent) throw Exception("Unable to send verification email")

        return ResponseEntity.ok(MessageResponse("Verification link sent to your email"))
    }

    @Operation(
        summary = "Verify email address",
        description = "Verifies the user's email address using the provided token."
    )
    @GetMapping
    fun verifyEmail(@RequestParam("token") token: String): RedirectView {
        val stored = oneTimeTokenService.consume(token, OneTimeToken.Purpose.VERIFY_EMAIL)
        val user = userService.findUser(stored.userId.toHexString())
            ?: throw BadRequestException("User not found")

        userService.emailVerified(user.id)

        return RedirectView("${appConfigProvider.websiteUrl}?emailVerified=true")
    }
}
