package com.vardansoft.authx.user.controller

import com.vardansoft.authx.core.config.UserContextService
import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.data.VerifyPhoneOtpRequest
import com.vardansoft.authx.data.UserResponse
import com.vardansoft.authx.user.service.UserService
import com.vardansoft.core.data.MessageResponse
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/phone-number")
class PhoneNumberController(
    private val userService: UserService,
    private val userContextService: UserContextService
) {

    @Operation(
        summary = "Initiate phone number update",
        description = "Initiates the process to update the phone number for the currently authenticated user. Sends an OTP to the new phone number."
    )
    @PostMapping("/update")
    fun initiatePhoneNumberUpdate(@Valid @RequestBody request: UpdatePhoneNumberRequest): ResponseEntity<MessageResponse> {
        val user = userContextService.getCurrentUser()
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
    @PostMapping("/verify")
    fun verifyPhoneNumberUpdate(
        @Valid @RequestBody request: VerifyPhoneOtpRequest
    ): ResponseEntity<UserResponse> {
        val user = userContextService.getCurrentUser()
        return try {
            ResponseEntity.ok(userService.verifyPhoneNumberUpdate(user.id, request))
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }
}