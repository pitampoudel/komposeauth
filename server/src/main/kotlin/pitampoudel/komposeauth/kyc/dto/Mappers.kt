package pitampoudel.komposeauth.kyc.dto

import kotlinx.datetime.toKotlinLocalDate
import pitampoudel.komposeauth.core.data.AddressInformation
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.data.PersonalInformation
import pitampoudel.komposeauth.kyc.entity.KycDocument
import pitampoudel.komposeauth.kyc.entity.KycVerification

/** [documentUrl] is where a stored file is handed out; the storage address on the record never is. */
fun KycVerification.toResponse(documentUrl: (KycDocument) -> String) = KycResponse(
    userId = userId.toHexString(),
    personalInformation = PersonalInformation(
        country = country,
        nationality = nationality,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        dateOfBirth = dateOfBirth.toKotlinLocalDate(),
        gender = gender,
        fatherName = fatherName,
        maritalStatus = maritalStatus,
        grandFatherName = grandFatherName
    ),
    currentAddress = AddressInformation(
        country = currentAddressCountry,
        state = currentAddressState,
        city = currentAddressCity,
        addressLine1 = currentAddressLine1,
        addressLine2 = currentAddressLine2,
    ),
    permanentAddress = AddressInformation(
        country = permanentAddressCountry,
        state = permanentAddressState,
        city = permanentAddressCity,
        addressLine1 = permanentAddressLine1,
        addressLine2 = permanentAddressLine2,
    ),
    documentInformation = KycResponse.DocumentInformationResponse(
        documentType = documentType,
        documentNumber = documentNumber,
        documentIssuedDate = documentIssuedDate?.toKotlinLocalDate(),
        documentExpiryDate = documentExpiryDate?.toKotlinLocalDate(),
        documentIssuedPlace = documentIssuedPlace,
        documentFrontUrl = documentFrontUrl?.let { documentUrl(KycDocument.FRONT) },
        documentBackUrl = documentBackUrl?.let { documentUrl(KycDocument.BACK) },
        selfieUrl = selfieUrl?.let { documentUrl(KycDocument.SELFIE) }
    ),
    status = status,
    thirdFactorWarnings = thirdFactorWarnings
)
