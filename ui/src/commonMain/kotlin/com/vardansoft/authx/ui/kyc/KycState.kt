package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.Country
import com.vardansoft.authx.data.DocumentInformation
import com.vardansoft.authx.domain.DocumentType
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.PersonalInformation
import com.vardansoft.authx.data.UpdateAddressDetailsRequest
import com.vardansoft.core.domain.KmpFile
import com.vardansoft.core.presentation.InfoMessage
import com.vardansoft.core.domain.validators.AuthXValidationError
import kotlinx.datetime.LocalDate

data class AddressState(
    val country: String = "",
    val countryError: AuthXValidationError? = null,
    val state: String = "",
    val stateError: AuthXValidationError? = null,
    val city: String = "",
    val cityError: AuthXValidationError? = null,
    val addressLine1: String = "",
    val addressLine1Error: AuthXValidationError? = null,
    val addressLine2: String = "",
    val addressLine2Error: AuthXValidationError? = null
) {
    fun hasError(): Boolean {
        return countryError != null ||
                stateError != null ||
                cityError != null ||
                addressLine1Error != null ||
                addressLine2Error != null
    }

    fun toRequest(): KycResponse.AddressInformation {
        require(!hasError()) { "Form contains errors" }

        return KycResponse.AddressInformation(
            country = country,
            state = state,
            city = city,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2
        )
    }


    companion object {
        fun fromData(data: KycResponse.AddressInformation): AddressState {
            return AddressState(
                country = data.country.orEmpty(),
                state = data.state.orEmpty(),
                city = data.city.orEmpty(),
                addressLine1 = data.addressLine1.orEmpty(),
                addressLine2 = data.addressLine2.orEmpty()
            )
        }
    }
}

data class PersonalInformationState(
    val country: String = "",
    val countryError: AuthXValidationError? = null,
    val nationality: String = "",
    val nationalityError: AuthXValidationError? = null,
    val firstName: String = "",
    val firstNameError: AuthXValidationError? = null,
    val middleName: String = "",
    val middleNameError: AuthXValidationError? = null,
    val lastName: String = "",
    val lastNameError: AuthXValidationError? = null,
    val dateOfBirth: LocalDate? = null,
    val dateOfBirthError: AuthXValidationError? = null,
    val gender: KycResponse.Gender? = null,
    val genderError: AuthXValidationError? = null,
    val fatherName: String = "",
    val fatherNameError: AuthXValidationError? = null,
    val grandFatherName: String = "",
    val grandFatherNameError: AuthXValidationError? = null,
    val motherName: String = "",
    val motherNameError: AuthXValidationError? = null,
    val grandMotherName: String = "",
    val grandMotherNameError: AuthXValidationError? = null,
    val maritalStatus: KycResponse.MaritalStatus? = null,
    val maritalStatusError: AuthXValidationError? = null,
    val occupation: String = "",
    val occupationError: AuthXValidationError? = null,
    val pan: String = "",
    val panError: AuthXValidationError? = null,
    val email: String = "",
    val emailError: AuthXValidationError? = null
) {

    fun hasError(): Boolean {
        return countryError != null ||
                nationalityError != null ||
                firstNameError != null ||
                middleNameError != null ||
                lastNameError != null ||
                dateOfBirthError != null ||
                genderError != null ||
                (fatherNameError != null && motherNameError != null) ||
                (grandFatherNameError != null && grandMotherNameError != null) ||
                maritalStatusError != null ||
                occupationError != null ||
                panError != null ||
                emailError != null
    }

    fun toRequest(): PersonalInformation {
        require(!hasError()) { "Form contains errors" }

        return PersonalInformation(
            country = country,
            nationality = nationality,
            firstName = firstName,
            middleName = middleName.takeIf { it.isNotBlank() },
            lastName = lastName,
            dateOfBirth = dateOfBirth!!,
            gender = gender!!,
            fatherName = fatherName,
            grandFatherName = grandFatherName,
            motherName = motherName,
            grandMotherName = grandMotherName,
            maritalStatus = maritalStatus!!,
            occupation = occupation,
            pan = pan,
            email = email
        )
    }
}

data class DocumentInformationState(
    val documentType: DocumentType? = null,
    val documentTypeError: AuthXValidationError? = null,
    val documentNumber: String = "",
    val documentNumberError: AuthXValidationError? = null,
    val documentIssuedDate: LocalDate? = null,
    val documentIssuedDateError: AuthXValidationError? = null,
    val documentIssuedPlace: String = "",
    val documentIssuedPlaceError: AuthXValidationError? = null,
    val documentExpiryDate: LocalDate? = null,
    val documentExpiryDateError: AuthXValidationError? = null,
    val documentFront: KmpFile? = null,
    val documentFrontError: AuthXValidationError? = null,
    val documentBack: KmpFile? = null,
    val documentBackError: AuthXValidationError? = null,
    val selfie: KmpFile? = null,
    val selfieError: AuthXValidationError? = null
) {
    fun hasError(): Boolean {
        return documentTypeError != null
                || documentNumberError != null
                || documentIssuedDateError != null
                || documentIssuedPlaceError != null
                || documentExpiryDateError != null
                || documentFrontError != null
                || documentBackError != null
                || selfieError != null
    }

    fun toRequest(): DocumentInformation {
        require(!hasError()) { "Form contains errors" }

        return DocumentInformation(
            documentType = documentType!!,
            documentNumber = documentNumber,
            documentIssuedDate = documentIssuedDate!!,
            documentExpiryDate = documentExpiryDate,
            documentIssuedPlace = documentIssuedPlace,
            documentFront = documentFront!!.toEncodedData(),
            documentBack = documentBack!!.toEncodedData(),
            selfie = selfie!!.toEncodedData()
        )
    }

}

data class KycState(
    val currentPage: Int? = 1,
    val personalInfo: PersonalInformationState = PersonalInformationState(),
    val documentInfo: DocumentInformationState = DocumentInformationState(),
    val permanentAddress: AddressState = AddressState(),
    val currentAddress: AddressState = AddressState(),
    val currentAddressSameAsPermanent: Boolean = false,
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val status: KycResponse.Status? = null,
    val countries: List<Country> = emptyList(),
    val occupations: List<String> = emptyList()
) {
    fun isSubmitted(): Boolean {
        return status in KycResponse.Status.submitted()
    }

    fun hasAddressDetailsError(): Boolean {
        val currentAddressError = if (!currentAddressSameAsPermanent) {
            currentAddress.hasError()
        } else {
            false
        }
        return currentAddressError || permanentAddress.hasError()
    }

    fun updateAddressDetailsRequest(): UpdateAddressDetailsRequest {

        require(!hasAddressDetailsError()) { "Form contains errors" }
        return UpdateAddressDetailsRequest(
            currentAddress = if (currentAddressSameAsPermanent) permanentAddress.toRequest() else currentAddress.toRequest(),
            permanentAddress = permanentAddress.toRequest(),
        )
    }
}