package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.JwtService
import pitampoudel.komposeauth.data.ApiEndpoints.VERIFY_EMAIL
import pitampoudel.komposeauth.user.service.UserService

@RestController
@RequestMapping("/$VERIFY_EMAIL")
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
        val user = userContextService.getUserFromAuthentication()

        // Check if user email exists
        if (user.email == null) {
            return ResponseEntity.badRequest().body(MessageResponse("User email is not set."))
        }

        val link = jwtService.generateEmailVerificationLink(userId = user.id.toHexString())

        val sent = emailService.sendSimpleMail(
            to = user.email,
            subject = "Verify Your Email",
            text = "Click the link to verify your email: $link"
        )
        if (!sent) throw Exception("Unable to send verification email")

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
