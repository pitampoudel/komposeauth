package com.vardansoft.authx.kyc.dto

import com.vardansoft.authx.data.AddressInformation
import com.vardansoft.authx.data.FamilyInformation
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.PersonalInformation
import com.vardansoft.authx.kyc.entity.KycVerification

fun KycVerification.toResponse() = KycResponse(
    id = id.toHexString(),
    userId = userId.toHexString(),
    personalInformation = PersonalInformation(
        nationality = nationality,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        gender = gender
    ),
    familyInformation = FamilyInformation(
        fatherName = fatherName,
        motherName = motherName,
        maritalStatus = maritalStatus
    ),
    currentAddress = AddressInformation(
        country = currentAddressCountry,
        province = currentAddressProvince,
        district = currentAddressDistrict,
        localUnit = currentAddressLocalUnit,
        wardNo = currentAddressWardNo,
        tole = currentAddressTole
    ),
    permanentAddress = AddressInformation(
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
