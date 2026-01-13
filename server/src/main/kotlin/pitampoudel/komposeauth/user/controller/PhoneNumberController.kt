package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.domain.ApiEndpoints.SEND_OTP
import pitampoudel.komposeauth.core.domain.ApiEndpoints.VERIFY_OTP
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.UserResponse
import pitampoudel.komposeauth.user.data.VerifyOtpRequest
import pitampoudel.komposeauth.user.service.UserService

@RestController
class PhoneNumberController(
    private val userService: UserService,
    private val userContextService: UserContextService
) {
    @Operation(summary = "Send OTP")
    @PostMapping("/$SEND_OTP")
    fun sendOtp(@Valid @RequestBody request: SendOtpRequest): ResponseEntity<MessageResponse> {
        return if (userService.sendOtp(request)) {
            ResponseEntity.ok(MessageResponse("An OTP has just been sent to ${request.phoneNumber}"))
        } else {
            ResponseEntity.badRequest().body(MessageResponse("Failed to send OTP"))
        }
    }

    @Operation(summary = "Verify OTP")
    @PostMapping("/$VERIFY_OTP")
    fun verifyPhoneNumber(
        @Valid @RequestBody request: VerifyOtpRequest
    ): UserResponse {
        val user = userContextService.getUserFromAuthentication()
        val phoneNumber = user.phoneNumber ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "User phone number is not set."
        )
        return userService.verifyPhoneNumber(user.id, phoneNumber, request.otp)
    }
}