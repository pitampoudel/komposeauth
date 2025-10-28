package pitampoudel.komposeauth.kyc

import pitampoudel.komposeauth.data.Country
import pitampoudel.komposeauth.data.DocumentInformation
import pitampoudel.komposeauth.domain.DocumentType
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.data.PersonalInformation
import pitampoudel.komposeauth.data.UpdateAddressDetailsRequest
import pitampoudel.core.domain.KmpFile
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.core.domain.validators.AuthValidationError
import kotlinx.datetime.LocalDate

data class AddressState(
    val country: String = "",
    val countryError: AuthValidationError? = null,
    val state: String = "",
    val stateError: AuthValidationError? = null,
    val city: String = "",
    val cityError: AuthValidationError? = null,
    val addressLine1: String = "",
    val addressLine1Error: AuthValidationError? = null,
    val addressLine2: String = "",
    val addressLine2Error: AuthValidationError? = null
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
    val countryError: AuthValidationError? = null,
    val nationality: String = "",
    val nationalityError: AuthValidationError? = null,
    val firstName: String = "",
    val firstNameError: AuthValidationError? = null,
    val middleName: String = "",
    val middleNameError: AuthValidationError? = null,
    val lastName: String = "",
    val lastNameError: AuthValidationError? = null,
    val dateOfBirth: LocalDate? = null,
    val dateOfBirthError: AuthValidationError? = null,
    val gender: KycResponse.Gender? = null,
    val genderError: AuthValidationError? = null,
    val fatherName: String = "",
    val fatherNameError: AuthValidationError? = null,
    val grandFatherName: String = "",
    val grandFatherNameError: AuthValidationError? = null,
    val motherName: String = "",
    val motherNameError: AuthValidationError? = null,
    val grandMotherName: String = "",
    val grandMotherNameError: AuthValidationError? = null,
    val maritalStatus: KycResponse.MaritalStatus? = null,
    val maritalStatusError: AuthValidationError? = null,
    val occupation: String = "",
    val occupationError: AuthValidationError? = null,
    val pan: String = "",
    val panError: AuthValidationError? = null,
    val email: String = "",
    val emailError: AuthValidationError? = null
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
    val documentTypeError: AuthValidationError? = null,
    val documentNumber: String = "",
    val documentNumberError: AuthValidationError? = null,
    val documentIssuedDate: LocalDate? = null,
    val documentIssuedDateError: AuthValidationError? = null,
    val documentIssuedPlace: String = "",
    val documentIssuedPlaceError: AuthValidationError? = null,
    val documentExpiryDate: LocalDate? = null,
    val documentExpiryDateError: AuthValidationError? = null,
    val documentFront: KmpFile? = null,
    val documentFrontError: AuthValidationError? = null,
    val documentBack: KmpFile? = null,
    val documentBackError: AuthValidationError? = null,
    val selfie: KmpFile? = null,
    val selfieError: AuthValidationError? = null
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