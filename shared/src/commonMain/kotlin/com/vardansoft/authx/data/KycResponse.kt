package com.vardansoft.authx.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class KycResponse(
    val id: String,
    val userId: String,
    val nationality: String,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val fatherName: String,
    val motherName: String,
    val maritalStatus: MaritalStatus,
    val documentType: DocumentType,
    val documentNumber: String,
    val documentIssuedDate: LocalDate,
    val documentExpiryDate: LocalDate,
    val documentIssuedPlace: String,
    val status: Status,
    val remarks: String? = null,
    val documentFrontUrl: String? = null,
    val documentBackUrl: String? = null,
    val selfieUrl: String? = null,
) {
    enum class Status { PENDING, APPROVED, REJECTED }
    enum class Gender { MALE, FEMALE, OTHER }
    enum class MaritalStatus { MARRIED, UNMARRIED }

}