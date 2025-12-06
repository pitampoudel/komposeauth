package pitampoudel.komposeauth.kyc.entity

import kotlinx.datetime.LocalDate
import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.domain.DocumentType
import java.time.Instant

@Document(collection = "kyc_verifications")
@TypeAlias("kyc_verification")
data class KycVerification(
    @Id
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
    val maritalStatus: KycResponse.MaritalStatus?,
    // OTHER
    val pan: String?,
    @field:Indexed(unique = true, partialFilter = $$"{ 'email': { '$type': 'string' } }")
    val email: String?,
    // DOCUMENT
    val documentType: DocumentType? = null,
    @field:Indexed(unique = true, partialFilter = $$"{ 'documentNumber': { '$type': 'string' } }")
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
    // CURRENT
    val currentAddressCountry: String? = null,
    val currentAddressState: String? = null,
    val currentAddressCity: String? = null,
    val currentAddressLine1: String? = null,
    val currentAddressLine2: String? = null,
    // OTHER
    val status: KycResponse.Status = KycResponse.Status.DRAFT,
    @CreatedDate val createdAt: Instant = Instant.now(),
    @LastModifiedDate val updatedAt: Instant = Instant.now()
)
