package com.vardansoft.authx.kyc.entity

import com.vardansoft.authx.data.DocumentType
import com.vardansoft.authx.data.KycResponse
import kotlinx.datetime.LocalDate
import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "kyc_verifications")
@TypeAlias("kyc_verification")
data class KycVerification(
    @Id val id: ObjectId = ObjectId.get(),
    @Indexed(unique = true)
    val userId: ObjectId,
    // PERSONAL
    val country: String,
    val nationality: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: KycResponse.Gender,
    // FAMILY
    val fatherName: String?,
    val grandFatherName: String?,
    val motherName: String?,
    val grandMotherName: String?,
    val maritalStatus: KycResponse.MaritalStatus?,
    // OTHER
    val occupation: String?,
    val pan: String?,
    val email: String?,
    // DOCUMENT
    val documentType: DocumentType? = null,
    val documentNumber: String? = null,
    val documentIssuedDate: LocalDate? = null,
    val documentExpiryDate: LocalDate? = null,
    val documentIssuedPlace: String? = null,
    val documentFrontUrl: String? = null,
    val documentBackUrl: String? = null,
    val selfieUrl: String? = null,
    // ADDRESS
    // PERMANENT
    val permanentAddressCountry: String? = null,
    val permanentAddressState: String? = null,
    val permanentAddressCity: String? = null,
    val permanentAddressLine1: String? = null,
    val permanentAddressLine2: String? = null,
    val permanentAddressPostalCode: String? = null,
    // CURRENT

    val currentAddressCountry: String? = null,
    val currentAddressState: String? = null,
    val currentAddressCity: String? = null,
    val currentAddressLine1: String? = null,
    val currentAddressLine2: String? = null,
    val currentAddressPostalCode: String? = null,
    // OTHER
    val status: KycResponse.Status = KycResponse.Status.PENDING,
    val remarks: String? = null,
    @CreatedDate val createdAt: Instant = Instant.now(),
    @LastModifiedDate val updatedAt: Instant = Instant.now()
)
