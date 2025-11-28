package pitampoudel.komposeauth.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import pitampoudel.core.data.EncodedData
import pitampoudel.komposeauth.domain.DocumentType

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
    val maritalStatus: KycResponse.MaritalStatus?,
    val pan: String?,
    val email: String?
)


@Serializable
data class DocumentInformation(
    val documentType: DocumentType,
    val documentNumber: String,
    val documentIssuedDate: LocalDate,
    val documentExpiryDate: LocalDate?,
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