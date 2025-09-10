package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.DocumentType
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.UpdateKycRequest
import com.vardansoft.core.domain.KmpFile
import kotlinx.datetime.LocalDate

data class KycState(
    val nationality: String = "",
    val nationalityError: String? = null,
    val firstName: String = "",
    val firstNameError: String? = null,
    val middleName: String = "",
    val middleNameError: String? = null,
    val lastName: String = "",
    val lastNameError: String? = null,
    val dateOfBirth: LocalDate? = null,
    val dateOfBirthError: String? = null,
    val gender: KycResponse.Gender? = null,
    val genderError: String? = null,
    val fatherName: String = "",
    val fatherNameError: String? = null,
    val motherName: String = "",
    val motherNameError: String? = null,
    val maritalStatus: KycResponse.MaritalStatus? = null,
    val maritalStatusError: String? = null,
    val documentType: DocumentType? = null,
    val documentTypeError: String? = null,
    val documentNumber: String = "",
    val documentNumberError: String? = null,
    val documentIssuedDate: LocalDate? = null,
    val documentIssuedDateError: String? = null,
    val documentExpiryDate: LocalDate? = null,
    val documentExpiryDateError: String? = null,
    val documentIssuedPlace: String = "",
    val documentIssuedPlaceError: String? = null,
    val documentFront: KmpFile? = null,
    val documentBack: KmpFile? = null,
    val selfie: KmpFile? = null,
    val progress: Float? = null,
    val infoMsg: String? = null,
    val existing: KycResponse? = null
) {
    fun containsError() = nationalityError != null || firstNameError != null
            || middleNameError != null || lastNameError != null || dateOfBirthError != null
            || genderError != null || fatherNameError != null || motherNameError != null
            || maritalStatusError != null || documentTypeError != null
            || documentNumberError != null || documentIssuedDateError != null ||
            documentExpiryDateError != null || documentIssuedPlaceError != null

    fun updateKycRequest(): UpdateKycRequest {
        require(!containsError()) { "Form contains errors" }
        return UpdateKycRequest(
            nationality = nationality,
            firstName = firstName,
            middleName = middleName.takeIf { it.isNotBlank() },
            lastName = lastName,
            dateOfBirth = dateOfBirth!!,
            gender = gender!!,
            fatherName = fatherName,
            motherName = motherName,
            maritalStatus = maritalStatus!!,
            documentType = documentType!!,
            documentNumber = documentNumber,
            documentIssuedDate = documentIssuedDate!!,
            documentExpiryDate = documentExpiryDate!!,
            documentIssuedPlace = documentIssuedPlace,
            documentFront = documentFront?.toEncodedData(),
            documentBack = documentBack?.toEncodedData(),
            selfie = selfie?.toEncodedData()
        )
    }

    val isApproved: Boolean get() = existing?.status == KycResponse.Status.APPROVED
    val isPending: Boolean get() = existing?.status == KycResponse.Status.PENDING
    val isRejected: Boolean get() = existing?.status == KycResponse.Status.REJECTED
}