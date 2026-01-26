package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.apache.coyote.BadRequestException
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.view.RedirectView
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.domain.ApiEndpoints.VERIFY_EMAIL
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.utils.findServerUrl
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService

@Controller
class EmailVerifyController(
    private val oneTimeTokenService: OneTimeTokenService,
    private val emailService: EmailService,
    private val userService: UserService,
    private val userContextService: UserContextService,
    private val appConfigService: AppConfigService
) {
    @Operation(
        summary = "Send verification email (link-based)",
        description = "Sends an email with a verification link to the currently authenticated user's email address."
    )
    @PostMapping
    @GetMapping("/$VERIFY_EMAIL")
    fun sendVerificationEmail(request: HttpServletRequest): ResponseEntity<MessageResponse> {
        val user = userContextService.getUserFromAuthentication()

        // Check if user email exists
        if (user.email == null) {
            return ResponseEntity.badRequest().body(MessageResponse("User email is not set."))
        }

        val link = oneTimeTokenService.generateEmailVerificationLink(
            userId = user.id,
            baseUrl = findServerUrl(request)
        )

        val sent = emailService.sendHtmlMail(
            baseUrl = findServerUrl(request),
            to = user.email,
            subject = "Verify Your Email",
            template = "email/generic",
            model = mapOf(
                "recipientName" to user.firstNameOrUser(),
                "message" to "Confirm that this is your email address to keep your account secure. This email will expire in 24 hours.",
                "actionUrl" to link,
                "actionText" to "Verify Your Email Address",
                "illustration" to "https://cdn.prod.website-files.com/643507075046cf6dcb169402/6538ece41f8e698f9d43b881_blog-image-for-cyber-security-for-businesses-in-australia-a-guide-6538ecc6a10e6.webp",
                "actionMessage" to "Click on Confirm your email address, and we'll move on!"
            )
        )
        if (!sent) throw Exception("Unable to send verification email")

        return ResponseEntity.ok(MessageResponse("Verification link sent to your email"))
    }

    @Operation(
        summary = "Verify email address (link)",
        description = "link-based email verification using one-time token."
    )
    @GetMapping("/$VERIFY_EMAIL")
    fun verifyEmail(@RequestParam("token") token: String): RedirectView {
        val stored = oneTimeTokenService.consume(token, OneTimeToken.Purpose.VERIFY_EMAIL)
        val user = userService.findUser(stored.userId.toHexString())
            ?: throw BadRequestException("User not found")

        userService.markEmailVerified(user, user.email!!)

        return RedirectView("${appConfigService.getConfig().websiteUrl}?emailVerified=true")
    }


}
