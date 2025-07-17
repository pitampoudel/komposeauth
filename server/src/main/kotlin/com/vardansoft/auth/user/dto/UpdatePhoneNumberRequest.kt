package com.vardansoft.auth.user.dto

import jakarta.validation.constraints.NotBlank

data class UpdatePhoneNumberRequest(
    @field:NotBlank(message = "Phone number is required")
    val phoneNumber: String,
    val countryCode: String? = null
)