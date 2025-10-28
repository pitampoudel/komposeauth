package pitampoudel.komposeauth.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import pitampoudel.komposeauth.domain.DocumentType


@Serializable
data class KycResponse(
    val userId: String,
    val personalInformation: PersonalInformation,
    val currentAddress: AddressInformation,
    val permanentAddress: AddressInformation,
    val documentInformation: DocumentInformationResponse,
    val status: Status
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
    )

    enum class Status {
        DRAFT, PENDING, APPROVED, REJECTED;

        companion object {
            fun submitted() = listOf(PENDING, APPROVED)
        }
    }

    enum class Gender { MALE, FEMALE, OTHER }
    enum class MaritalStatus { MARRIED, UNMARRIED }

}