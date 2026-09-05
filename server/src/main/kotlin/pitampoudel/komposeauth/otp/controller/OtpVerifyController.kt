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
import pitampoudel.komposeauth.core.security.ratelimit.RateLimitProperties
import pitampoudel.komposeauth.core.security.ratelimit.RateLimiter
import pitampoudel.komposeauth.core.service.email.EmailVerificationService
import pitampoudel.komposeauth.core.utils.findServerUrl
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
    private val rateLimiter: RateLimiter,
    private val rateLimitProperties: RateLimitProperties
) {

    /**
     * Caps how often one address or phone number can be targeted, whoever asks. The per-IP filter
     * alone doesn't stop a distributed sender from flooding a single victim — or from running up an
     * SMS bill by pointing the endpoint at a premium-rate number.
     */
    private fun enforceTargetQuota(target: String) {
        if (!rateLimitProperties.enabled) return
        rateLimiter.enforce(
            key = "otp-target:$target",
            limit = rateLimitProperties.otpPerTarget.limit,
            window = rateLimitProperties.otpPerTarget.window,
            message = "Too many verification codes requested for this address."
        )
    }

    @Operation(summary = "Send OTP")
    @PostMapping("/${ApiEndpoints.SEND_OTP}")
    fun sendOtp(
        @Valid @RequestBody request: SendOtpRequest,
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<MessageResponse> {

        val authenticatedUser = userContextService.authenticatedUserOrNull()
        val response = when (request.type) {
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
        return ResponseEntity.ok(response)

    }


    @Operation(summary = "Verify OTP")
    @PostMapping("/${ApiEndpoints.VERIFY_OTP}")
    fun verifyOTP(
        @RequestBody request: VerifyOtpRequest
    ): UserResponse {
        val user = userContextService.getUserFromAuthentication()
        return if (request.type == OtpType.PHONE) {
            /*
             * Parsed to E.164 here exactly as `sendOtp` does above, because the number is the key the
             * code was filed under.
             *
             * A client that asks for a code on "9812345678" and then verifies the same string it
             * showed the user was looking up an OTP that was stored as "+9779812345678" — no record,
             * so a correct code came back "invalid or expired", and any client that did send the E.164
             * form on both legs stored a number whose spelling depended on which screen wrote it.
             * Normalising both ends makes the two calls agree whatever the user typed.
             */
            val parsedPhone = parsePhoneNumber(null, request.username)
                ?: throw IllegalArgumentException("Invalid phone number format")
            val phoneNumber = parsedPhone.fullNumberInE164Format
            // The same rule the send leg enforces: a number that already belongs to somebody else is
            // not yours to attach, whichever leg you arrive on.
            enforceSelfRequest(currentUser = user, targetUsername = phoneNumber)
            userService.verifyPhoneNumber(user.id, phoneNumber, request.otp)
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