package pitampoudel.komposeauth.otp.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.MessageResponse
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.security.ratelimit.RateLimiter
import pitampoudel.komposeauth.core.service.email.EmailVerificationService
import pitampoudel.komposeauth.core.utils.findServerUrl
import java.time.Duration
import pitampoudel.komposeauth.otp.service.PhoneNumberVerificationService
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.UserResponse
import pitampoudel.komposeauth.user.data.VerifyOtpRequest
import pitampoudel.komposeauth.user.domain.OtpType
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService

@RestController
class OtpVerifyController(
    private val userService: UserService,
    private val userContextService: UserContextService,
    val emailVerificationService: EmailVerificationService,
    val phoneNumberVerificationService: PhoneNumberVerificationService,
    private val rateLimiter: RateLimiter
) {

    /**
     * Caps how often one address or phone number can be targeted, whoever asks. The per-IP filter
     * alone doesn't stop a distributed sender from flooding a single victim — or from running up an
     * SMS bill by pointing the endpoint at a premium-rate number.
     */
    private fun enforceTargetQuota(target: String) {
        rateLimiter.enforce(
            key = "otp-target:$target",
            limit = 5,
            window = Duration.ofHours(1),
            message = "Too many verification codes requested for this address. Try again later."
        )
    }

    @Operation(summary = "Send OTP")
    @PostMapping("/${ApiEndpoints.SEND_OTP}")
    fun sendOtp(
        @Valid @RequestBody request: SendOtpRequest,
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<MessageResponse> {

        val authenticatedUser = userContextService.authenticatedUserOrNull()
        when (request.type) {
            OtpType.PHONE -> {
                val parsedPhone = parsePhoneNumber(null, request.username)
                    ?: throw IllegalArgumentException("Invalid phone number format")
                enforceSelfRequest(currentUser = authenticatedUser, targetUsername = parsedPhone.fullNumberInE164Format)
                enforceTargetQuota(parsedPhone.fullNumberInE164Format)
                phoneNumberVerificationService.initiate(
                    phoneNumber = parsedPhone.fullNumberInE164Format
                )
            }

            OtpType.EMAIL -> {
                val normalizedEmail = request.username.lowercase()
                enforceSelfRequest(currentUser = authenticatedUser, targetUsername = normalizedEmail)
                enforceTargetQuota(normalizedEmail)
                emailVerificationService.initiate(
                    email = request.username,
                    baseUrl = findServerUrl(httpServletRequest)
                )
            }
        }
        return ResponseEntity.ok(MessageResponse("An OTP has just been sent to ${request.username}"))

    }


    @Operation(summary = "Verify OTP")
    @PostMapping("/${ApiEndpoints.VERIFY_OTP}")
    fun verifyOTP(
        @RequestBody request: VerifyOtpRequest
    ): UserResponse {
        val user = userContextService.getUserFromAuthentication()
        return if (request.type == OtpType.PHONE) {
            userService.verifyPhoneNumber(user.id, request.username, request.otp)
        } else {
            userService.verifyEmail(user.id, request.username, request.otp)
        }

    }

    private fun enforceSelfRequest(currentUser: User?, targetUsername: String) {
        if (currentUser == null) return
        val targetOwner = userService.findByUserName(targetUsername)
        if (targetOwner != null && targetOwner.id != currentUser.id) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You can't request an OTP for already used email/phone"
            )
        }
    }
}