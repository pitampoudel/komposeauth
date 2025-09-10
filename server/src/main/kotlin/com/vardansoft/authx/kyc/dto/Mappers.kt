package com.vardansoft.authx.kyc.dto

import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.kyc.entity.KycVerification

fun KycVerification.toResponse() = KycResponse(
    id = id.toHexString(),
    userId = userId.toHexString(),
    nationality = nationality,
    firstName = firstName,
    middleName = middleName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    gender = gender,
    fatherName = fatherName,
    motherName = motherName,
    maritalStatus = maritalStatus,
    documentType = documentType,
    documentNumber = documentNumber,
    documentIssuedDate = documentIssuedDate,
    documentExpiryDate = documentExpiryDate,
    documentIssuedPlace = documentIssuedPlace,
    status = status,
    remarks = remarks,
    documentFrontUrl = documentFrontUrl,
    documentBackUrl = documentBackUrl,
    selfieUrl = selfieUrl
)
