package com.vardansoft.authx.data

import com.vardansoft.core.data.EncodedData
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class PersonalInformation(
    val nationality: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: KycResponse.Gender,
)

@Serializable
data class AddressInformation(
    val country: String,
    val province: String,
    val district: String,
    val localUnit: String,
    val wardNo: String,
    val tole: String
)

@Serializable
data class FamilyInformation(
    val fatherName: String,
    val motherName: String,
    val maritalStatus: KycResponse.MaritalStatus,
)

@Serializable
data class DocumentInformation(
    val documentType: DocumentType,
    val documentNumber: String,
    val documentIssuedDate: LocalDate,
    val documentExpiryDate: LocalDate,
    val documentIssuedPlace: String,
    val documentFront: EncodedData,
    val documentBack: EncodedData,
    val selfie: EncodedData
)

@Serializable
data class UpdateKycRequest(
    val personalInformation: PersonalInformation,
    val familyInformation: FamilyInformation,
    val currentAddress: AddressInformation,
    val permanentAddress: AddressInformation,
    val documentInformation: DocumentInformation
)