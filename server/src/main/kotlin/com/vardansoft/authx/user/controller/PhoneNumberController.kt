package com.vardansoft.authx.user.controller

import com.vardansoft.authx.core.config.UserContextService
import com.vardansoft.authx.user.dto.UpdatePhoneNumberRequest
import com.vardansoft.authx.user.dto.UserResponse
import com.vardansoft.authx.user.dto.VerifyPhoneOtpRequest
import com.vardansoft.authx.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/phone-number")
class PhoneNumberController(
    private val userService: UserService,
    private val userContextService: UserContextService
) {

    @PostMapping("/update")
    fun initiatePhoneNumberUpdate(@Valid @RequestBody request: UpdatePhoneNumberRequest): ResponseEntity<*> {
        val user = userContextService.getCurrentUser()
        val success = userService.initiatePhoneNumberUpdate(user, request)
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "An OTP has just been sent to ${request.phoneNumber}"))
        } else {
            ResponseEntity.badRequest().body(mapOf("error" to "Failed to send OTP"))
        }
    }

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