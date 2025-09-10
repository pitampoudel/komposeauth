package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.AddressInformation
import com.vardansoft.authx.data.DocumentInformation
import com.vardansoft.authx.data.DocumentType
import com.vardansoft.authx.data.FamilyInformation
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.PersonalInformation
import com.vardansoft.authx.data.UpdateKycRequest
import com.vardansoft.core.domain.KmpFile
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
    fun containsError(): Boolean {
        return countryError != null ||
                provinceError != null ||
                districtError != null ||
                localUnitError != null ||
                wardNoError != null ||
                toleError != null
    }

    fun toRequestData(): AddressInformation = AddressInformation(
        country = country,
        province = province,
        district = district,
        localUnit = localUnit,
        wardNo = wardNo,
        tole = tole
    )

    companion object {
        fun fromData(data: AddressInformation): AddressState {
            return AddressState(
                country = data.country,
                province = data.province,
                district = data.district,
                localUnit = data.localUnit,
                wardNo = data.wardNo,
                tole = data.tole
            )
        }
    }
}

data class PersonalInformationState(
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
    val genderError: String? = null
) {
    fun containsError(): Boolean {
        return nationalityError != null ||
                firstNameError != null ||
                middleNameError != null ||
                lastNameError != null ||
                dateOfBirthError != null ||
                genderError != null
    }

    fun toRequestData(): PersonalInformation = PersonalInformation(
        nationality = nationality,
        firstName = firstName,
        middleName = middleName.takeIf { it.isNotBlank() },
        lastName = lastName,
        dateOfBirth = dateOfBirth!!,
        gender = gender!!
    )
}

data class FamilyInformationState(
    val fatherName: String = "",
    val fatherNameError: String? = null,
    val motherName: String = "",
    val motherNameError: String? = null,
    val maritalStatus: KycResponse.MaritalStatus? = null,
    val maritalStatusError: String? = null
) {
    fun containsError(): Boolean {
        return fatherNameError != null
                || motherNameError != null
                || maritalStatusError != null
    }

    fun toRequestData(): FamilyInformation = FamilyInformation(
        fatherName = fatherName,
        motherName = motherName,
        maritalStatus = maritalStatus!!
    )
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
    fun containsError(): Boolean {
        return documentTypeError != null
                || documentNumberError != null
                || documentIssuedDateError != null
                || documentIssuedPlaceError != null
                || documentExpiryDateError != null
                || documentFrontError != null
                || documentBackError != null
                || selfieError != null
    }

    fun toRequestData(): DocumentInformation = DocumentInformation(
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

data class KycState(
    val personalInfo: PersonalInformationState = PersonalInformationState(),
    val familyInfo: FamilyInformationState = FamilyInformationState(),
    val documentInfo: DocumentInformationState = DocumentInformationState(),
    val permanentAddress: AddressState = AddressState(),
    val currentAddress: AddressState = AddressState(),
    val currentAddressSameAsPermanent: Boolean = false,
    val progress: Float? = null,
    val infoMsg: String? = null,
    val existing: KycResponse? = null
) {
    fun containsError(): Boolean {
        val currentAddressError = if (!currentAddressSameAsPermanent) {
            currentAddress.containsError()
        } else {
            false
        }

        return personalInfo.containsError() ||
                familyInfo.containsError() ||
                documentInfo.containsError() ||
                currentAddressError ||
                permanentAddress.containsError()
    }

    fun updateKycRequest(): UpdateKycRequest {
        require(!containsError()) { "Form contains errors" }
        return UpdateKycRequest(
            personalInformation = personalInfo.toRequestData(),
            familyInformation = familyInfo.toRequestData(),
            currentAddress = if (currentAddressSameAsPermanent) permanentAddress.toRequestData() else currentAddress.toRequestData(),
            permanentAddress = permanentAddress.toRequestData(),
            documentInformation = documentInfo.toRequestData()
        )
    }

    val isApproved: Boolean get() = existing?.status == KycResponse.Status.APPROVED
    val isPending: Boolean get() = existing?.status == KycResponse.Status.PENDING
    val isRejected: Boolean get() = existing?.status == KycResponse.Status.REJECTED
}
