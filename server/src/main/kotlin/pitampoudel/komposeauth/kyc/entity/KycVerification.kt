package pitampoudel.komposeauth.kyc.entity

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import kotlinx.datetime.LocalDate
import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.domain.DocumentType
import java.time.Instant

@Document(collection = "kyc_verifications")
@TypeAlias("kyc_verification")
@CompoundIndexes(
    CompoundIndex(
        name = "status_created_idx",
        def = "{'status': 1, 'createdAt': -1}"
    ),
    CompoundIndex(
        name = "country_status_idx",
        def = "{'country': 1, 'status': 1}"
    )
)
data class KycVerification(
    @Id
    val userId: ObjectId,
    // PERSONAL
    @field:NotBlank(message = "Country is required")
    val country: String,
    @field:NotBlank(message = "Nationality is required")
    val nationality: String,
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    val middleName: String?,
    @field:NotBlank(message = "Last name is required")
    val lastName: String,
    @field:PastOrPresent(message = "Date of birth cannot be in the future")
    val dateOfBirth: LocalDate,
    val gender: KycResponse.Gender,
    // FAMILY
    val fatherName: String?,
    val grandFatherName: String?,
    val maritalStatus: KycResponse.MaritalStatus?,
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
    @Indexed
    val status: KycResponse.Status = KycResponse.Status.DRAFT,
    @CreatedDate val createdAt: Instant = Instant.now(),
    @LastModifiedDate val updatedAt: Instant = Instant.now()
)
