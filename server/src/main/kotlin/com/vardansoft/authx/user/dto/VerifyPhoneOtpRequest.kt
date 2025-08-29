package com.vardansoft.authx.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class VerifyPhoneOtpRequest(
    val countryCode: String?,
    @field:NotBlank(message = "Phone number is required")
    val phoneNumber: String,
    @field:NotBlank(message = "OTP is required")
    @field:Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    val otp: String
)