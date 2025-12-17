package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.data.ApiEndpoints.UPDATE_PHONE_NUMBER
import pitampoudel.komposeauth.core.data.ApiEndpoints.VERIFY_PHONE_NUMBER
import pitampoudel.komposeauth.core.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.core.data.UserResponse
import pitampoudel.komposeauth.core.data.VerifyPhoneOtpRequest
import pitampoudel.komposeauth.user.service.UserService

@RestController
class PhoneNumberController(
    private val userService: UserService,
    private val userContextService: UserContextService
) {

    @Operation(
        summary = "Initiate phone number update",
        description = "Initiates the process to update the phone number for the currently authenticated user. Sends an OTP to the new phone number."
    )
    @PostMapping("/$UPDATE_PHONE_NUMBER")
    fun initiatePhoneNumberUpdate(@Valid @RequestBody request: UpdatePhoneNumberRequest): ResponseEntity<MessageResponse> {
        val user = userContextService.getUserFromAuthentication()
        val success = userService.initiatePhoneNumberUpdate(request)
        return if (success) {
            ResponseEntity.ok(MessageResponse("An OTP has just been sent to ${request.phoneNumber}"))
        } else {
            ResponseEntity.badRequest().body(MessageResponse("Failed to send OTP"))
        }
    }

    @Operation(
        summary = "Verify phone number update",
        description = "Verifies the phone number update using the OTP sent to the new phone number."
    )
    @PostMapping("/$VERIFY_PHONE_NUMBER")
    fun verifyPhoneNumberUpdate(
        @Valid @RequestBody request: VerifyPhoneOtpRequest
    ): ResponseEntity<UserResponse> {
        val user = userContextService.getUserFromAuthentication()
        return try {
            ResponseEntity.ok(userService.verifyPhoneNumberUpdate(user.id, request))
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }
}