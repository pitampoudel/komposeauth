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
    documentIssuedDate = documentIssuedDate,
    documentExpiryDate = documentExpiryDate,
    documentIssuedPlace = documentIssuedPlace,
    status = status,
    remarks = remarks,
    documentFrontUrl = documentFrontUrl,
    documentBackUrl = documentBackUrl,
    selfieUrl = selfieUrl
)
