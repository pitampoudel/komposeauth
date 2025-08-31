package com.vardansoft.authx.kyc.dto

import com.vardansoft.authx.kyc.entity.KycVerification

data class KycResponse(
    val id: String,
    val userId: String,
    val fullName: String,
    val documentType: String,
    val documentNumber: String,
    val country: String,
    val status: KycVerification.Status,
    val rejectionReason: String?,
    val documentFrontUrl: String?,
    val documentBackUrl: String?,
    val selfieUrl: String?,
)