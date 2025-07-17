package com.vardansoft.auth.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class VerifyPhoneOtpRequest(
    @field:NotBlank(message = "OTP is required")
    @field:Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    val otp: String
)