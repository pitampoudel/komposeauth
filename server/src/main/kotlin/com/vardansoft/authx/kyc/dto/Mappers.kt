package com.vardansoft.authx.kyc.dto

import com.vardansoft.authx.kyc.entity.KycVerification

fun KycVerification.toResponse() = KycResponse(
    id = id.toHexString(),
    userId = userId.toHexString(),
    fullName = fullName,
    documentType = documentType,
    documentNumber = documentNumber,
    country = country,
    status = status,
    rejectionReason = remarks,
    documentFrontUrl = documentFrontUrl,
    documentBackUrl = documentBackUrl,
    selfieUrl = selfieUrl
)
