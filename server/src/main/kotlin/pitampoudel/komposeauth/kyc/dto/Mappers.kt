package pitampoudel.komposeauth.kyc.dto

import pitampoudel.komposeauth.core.data.KycResponse
import pitampoudel.komposeauth.core.data.PersonalInformation
import pitampoudel.komposeauth.kyc.entity.KycVerification

fun KycVerification.toResponse() = KycResponse(
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
        maritalStatus = maritalStatus,
        grandFatherName = grandFatherName
    ),
    currentAddress = KycResponse.AddressInformation(
        country = currentAddressCountry,
        state = currentAddressState,
        city = currentAddressCity,
        addressLine1 = currentAddressLine1,
        addressLine2 = currentAddressLine2,
    ),
    permanentAddress = KycResponse.AddressInformation(
        country = permanentAddressCountry,
        state = permanentAddressState,
        city = permanentAddressCity,
        addressLine1 = permanentAddressLine1,
        addressLine2 = permanentAddressLine2,
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
    status = status
)
