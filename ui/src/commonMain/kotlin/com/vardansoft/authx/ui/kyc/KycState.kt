package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.DocumentInformation
import com.vardansoft.authx.data.DocumentType
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.PersonalInformation
import com.vardansoft.authx.data.UpdateAddressDetailsRequest
import com.vardansoft.core.domain.KmpFile
import com.vardansoft.core.presentation.InfoMessage
import kotlinx.datetime.LocalDate

data class AddressState(
    val country: String = "",
    val countryError: String? = null,
    val province: String = "",
    val provinceError: String? = null,
    val district: String = "",
    val districtError: String? = null,
    val localUnit: String = "",
    val localUnitError: String? = null,
    val wardNo: String = "",
    val wardNoError: String? = null,
    val tole: String = "",
    val toleError: String? = null
) {
    fun hasError(): Boolean {
        return countryError != null ||
                provinceError != null ||
                districtError != null ||
                localUnitError != null ||
                wardNoError != null ||
                toleError != null
    }

    fun toRequest(): KycResponse.AddressInformation {
        require(!hasError()) { "Form contains errors" }

        return KycResponse.AddressInformation(
            country = country,
            province = province,
            district = district,
            localUnit = localUnit,
            wardNo = wardNo,
            tole = tole
        )
    }


    companion object {
        fun fromData(data: KycResponse.AddressInformation): AddressState {
            return AddressState(
                country = data.country.orEmpty(),
                province = data.province.orEmpty(),
                district = data.district.orEmpty(),
                localUnit = data.localUnit.orEmpty(),
                wardNo = data.wardNo.orEmpty(),
                tole = data.tole.orEmpty()
            )
        }
    }
}

data class PersonalInformationState(
    val country: String = "",
    val countryError: String? = null,
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
    val maritalStatusError: String? = null
) {

    fun hasError(): Boolean {
        return countryError!=null ||
                nationalityError != null ||
                firstNameError != null ||
                middleNameError != null ||
                lastNameError != null ||
                dateOfBirthError != null ||
                genderError != null ||
                fatherNameError != null ||
                motherNameError != null ||
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
            motherName = motherName,
            maritalStatus = maritalStatus!!
        )
    }
}

data class DocumentInformationState(
    val documentType: DocumentType? = null,
    val documentTypeError: String? = null,
    val documentNumber: String = "",
    val documentNumberError: String? = null,
    val documentIssuedDate: LocalDate? = null,
    val documentIssuedDateError: String? = null,
    val documentIssuedPlace: String = "",
    val documentIssuedPlaceError: String? = null,
    val documentExpiryDate: LocalDate? = null,
    val documentExpiryDateError: String? = null,
    val documentFront: KmpFile? = null,
    val documentFrontError: String? = null,
    val documentBack: KmpFile? = null,
    val documentBackError: String? = null,
    val selfie: KmpFile? = null,
    val selfieError: String? = null
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
            documentExpiryDate = documentExpiryDate!!,
            documentIssuedPlace = documentIssuedPlace,
            documentFront = documentFront!!.toEncodedData(),
            documentBack = documentBack!!.toEncodedData(),
            selfie = selfie!!.toEncodedData()
        )
    }

}

data class KycState(
    val currentPage: Int = 1,
    val personalInfo: PersonalInformationState = PersonalInformationState(),
    val documentInfo: DocumentInformationState = DocumentInformationState(),
    val permanentAddress: AddressState = AddressState(),
    val currentAddress: AddressState = AddressState(),
    val currentAddressSameAsPermanent: Boolean = false,
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val status: KycResponse.Status? = null
) {
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