package pitampoudel.komposeauth.kyc

import pitampoudel.komposeauth.core.data.CountryResponse
import pitampoudel.komposeauth.core.data.DocumentInformation
import pitampoudel.komposeauth.core.domain.DocumentType
import pitampoudel.komposeauth.core.data.KycResponse
import pitampoudel.komposeauth.core.data.PersonalInformation
import pitampoudel.komposeauth.core.data.UpdateAddressDetailsRequest
import pitampoudel.core.domain.KmpFile
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.core.domain.validators.GeneralValidationError
import kotlinx.datetime.LocalDate

data class AddressState(
    val country: String = "",
    val countryError: GeneralValidationError? = null,
    val state: String = "",
    val stateError: GeneralValidationError? = null,
    val city: String = "",
    val cityError: GeneralValidationError? = null,
    val addressLine1: String = "",
    val addressLine1Error: GeneralValidationError? = null,
    val addressLine2: String = "",
    val addressLine2Error: GeneralValidationError? = null
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
    val countryError: GeneralValidationError? = null,
    val nationality: String = "",
    val nationalityError: GeneralValidationError? = null,
    val firstName: String = "",
    val firstNameError: GeneralValidationError? = null,
    val middleName: String = "",
    val middleNameError: GeneralValidationError? = null,
    val lastName: String = "",
    val lastNameError: GeneralValidationError? = null,
    val dateOfBirth: LocalDate? = null,
    val dateOfBirthError: GeneralValidationError? = null,
    val gender: KycResponse.Gender? = null,
    val genderError: GeneralValidationError? = null,
    val fatherName: String = "",
    val fatherNameError: GeneralValidationError? = null,
    val grandFatherName: String = "",
    val grandFatherNameError: GeneralValidationError? = null,
    val maritalStatus: KycResponse.MaritalStatus? = null,
    val maritalStatusError: GeneralValidationError? = null
) {

    fun hasError(): Boolean {
        return countryError != null ||
                nationalityError != null ||
                firstNameError != null ||
                middleNameError != null ||
                lastNameError != null ||
                dateOfBirthError != null ||
                genderError != null ||
                fatherNameError != null ||
                grandFatherNameError != null ||
                maritalStatusError != null
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
            maritalStatus = maritalStatus!!
        )
    }
}

data class DocumentInformationState(
    val documentType: DocumentType? = null,
    val documentTypeError: GeneralValidationError? = null,
    val documentNumber: String = "",
    val documentNumberError: GeneralValidationError? = null,
    val documentIssuedDate: LocalDate? = null,
    val documentIssuedDateError: GeneralValidationError? = null,
    val documentIssuedPlace: String = "",
    val documentIssuedPlaceError: GeneralValidationError? = null,
    val documentExpiryDate: LocalDate? = null,
    val documentExpiryDateError: GeneralValidationError? = null,
    val documentFront: KmpFile? = null,
    val documentFrontError: GeneralValidationError? = null,
    val documentBack: KmpFile? = null,
    val documentBackError: GeneralValidationError? = null,
    val selfie: KmpFile? = null,
    val selfieError: GeneralValidationError? = null
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
    val countries: List<CountryResponse> = emptyList()) {
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