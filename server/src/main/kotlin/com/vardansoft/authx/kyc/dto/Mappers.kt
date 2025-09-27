package com.vardansoft.authx.kyc.dto

import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.PersonalInformation
import com.vardansoft.authx.kyc.entity.KycVerification

fun KycVerification.toResponse() = KycResponse(
    id = id.toHexString(),
    userId = userId.toHexString(),
    personalInformation = PersonalInformation(
        country = country,
        nationality = nationality,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        gender = gender,
        fatherName = fatherName,
        motherName = motherName,
        maritalStatus = maritalStatus,
        grandFatherName = grandFatherName,
        grandMotherName = grandMotherName,
        occupation = occupation,
        pan = pan,
        email = email
    ),
    currentAddress = KycResponse.AddressInformation(
        country = currentAddressCountry,
        province = currentAddressProvince,
        district = currentAddressDistrict,
        localUnit = currentAddressLocalUnit,
        wardNo = currentAddressWardNo,
        tole = currentAddressTole
    ),
    permanentAddress = KycResponse.AddressInformation(
        country = permanentAddressCountry,
        province = permanentAddressProvince,
        district = permanentAddressDistrict,
        localUnit = permanentAddressLocalUnit,
        wardNo = permanentAddressWardNo,
        tole = permanentAddressTole
    ),
    documentInformation = KycResponse.DocumentInformationResponse(
        documentType = documentType,
        documentNumber = documentNumber,
        documentIssuedDate = documentIssuedDate,
        documentExpiryDate = documentExpiryDate,
        documentIssuedPlace = documentIssuedPlace,
        documentFrontUrl = documentFrontUrl,
        documentBackUrl = documentBackUrl,
        selfieUrl = selfieUrl
    ),
    status = status,
    remarks = remarks
)
