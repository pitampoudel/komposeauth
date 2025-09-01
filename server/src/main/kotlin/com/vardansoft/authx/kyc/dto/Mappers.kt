package com.vardansoft.authx.kyc.dto

import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.kyc.entity.KycVerification

fun KycVerification.toResponse() = KycResponse(
    id = id.toHexString(),
    userId = userId.toHexString(),
    fullName = fullName,
    documentType = documentType,
    documentNumber = documentNumber,
    country = country,
    status = status,
    remarks = remarks,
    documentFrontUrl = documentFrontUrl,
    documentBackUrl = documentBackUrl,
    selfieUrl = selfieUrl
)
