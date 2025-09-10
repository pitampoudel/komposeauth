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
    @Indexed(unique = true) val userId: ObjectId,
    // PERSONAL
    val nationality: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: KycResponse.Gender,
    // FAMILY
    val fatherName: String,
    val motherName: String,
    val maritalStatus: KycResponse.MaritalStatus,
    // DOCUMENT
    val documentType: DocumentType,
    val documentNumber: String,
    val documentIssuedDate: LocalDate,
    val documentExpiryDate: LocalDate,
    val documentIssuedPlace: String,
    val documentFrontUrl: String,
    val documentBackUrl: String,
    val selfieUrl: String,
    // ADDRESS
    // PERMANENT
    val permanentAddressTole: String,
    val permanentAddressWardNo: String,
    val permanentAddressLocalUnit: String,
    val permanentAddressDistrict: String,
    val permanentAddressProvince: String,
    val permanentAddressCountry: String,
    // CURRENT
    val currentAddressTole: String,
    val currentAddressWardNo: String,
    val currentAddressLocalUnit: String,
    val currentAddressDistrict: String,
    val currentAddressProvince: String,
    val currentAddressCountry: String,
    // OTHER
    val status: KycResponse.Status = KycResponse.Status.PENDING,
    val remarks: String? = null,
    @CreatedDate val createdAt: Instant = Instant.now(),
    @LastModifiedDate val updatedAt: Instant = Instant.now()
)
