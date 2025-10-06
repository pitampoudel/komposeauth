package com.vardansoft.authx.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable


@Serializable
data class KycResponse(
    val id: String,
    val userId: String,
    val personalInformation: PersonalInformation,
    val currentAddress: AddressInformation,
    val permanentAddress: AddressInformation,
    val documentInformation: DocumentInformationResponse,
    val status: Status,
    val remarks: String?
) {

    @Serializable
    data class DocumentInformationResponse(
        val documentType: DocumentType?,
        val documentNumber: String?,
        val documentIssuedDate: LocalDate?,
        val documentExpiryDate: LocalDate?,
        val documentIssuedPlace: String?,
        val documentFrontUrl: String?,
        val documentBackUrl: String?,
        val selfieUrl: String?
    )

    @Serializable
    data class AddressInformation(
        val country: String?,
        val state: String?,
        val city: String?,
        val addressLine1: String?,
        val addressLine2: String?,
        val postalCode: String?,
    )

    enum class Status { PENDING, APPROVED, REJECTED }
    enum class Gender { MALE, FEMALE, OTHER }
    enum class MaritalStatus { MARRIED, UNMARRIED }

}