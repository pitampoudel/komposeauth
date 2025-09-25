package com.vardansoft.authx.data

import com.vardansoft.core.data.EncodedData
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class PersonalInformation(
    val country: String,
    val nationality: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: KycResponse.Gender,
    val fatherName: String?,
    val grandFatherName: String?,
    val motherName: String?,
    val grandMotherName: String?,
    val maritalStatus: KycResponse.MaritalStatus
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
data class UpdateAddressDetailsRequest(
    val currentAddress: KycResponse.AddressInformation,
    val permanentAddress: KycResponse.AddressInformation,
)