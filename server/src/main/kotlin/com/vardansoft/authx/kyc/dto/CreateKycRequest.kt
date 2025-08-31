package com.vardansoft.authx.kyc.dto

import jakarta.validation.constraints.NotBlank

data class CreateKycRequest(
    @field:NotBlank
    val fullName: String,
    @field:NotBlank
    val documentType: String,
    @field:NotBlank
    val documentNumber: String,
    @field:NotBlank
    val country: String,
    val documentFrontUrl: String? = null,
    val documentBackUrl: String? = null,
    val selfieUrl: String? = null,
)