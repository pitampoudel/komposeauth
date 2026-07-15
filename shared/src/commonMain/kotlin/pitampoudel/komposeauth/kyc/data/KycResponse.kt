package pitampoudel.komposeauth.kyc.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import pitampoudel.komposeauth.core.data.AddressInformation
import pitampoudel.komposeauth.kyc.domain.DocumentType

@Serializable
data class KycResponse(
    val userId: String,
    val personalInformation: PersonalInformation,
    val currentAddress: AddressInformation,
    val permanentAddress: AddressInformation,
    val documentInformation: DocumentInformationResponse,
    val status: Status,
    /**
     * Reasons a reviewer should look closer at a Third Factor submission — a failed verdict, a
     * bypassed step, a weak face match, or a document that disagrees with what the user declared.
     * Empty for manual submissions and for sessions where nothing stood out.
     */
    val thirdFactorWarnings: List<String> = emptyList()
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


    enum class Status {
        DRAFT, PENDING, APPROVED, REJECTED;

        companion object {
            fun submitted() = listOf(PENDING, APPROVED)
        }
    }

    enum class Gender { MALE, FEMALE, OTHER }
    enum class MaritalStatus { MARRIED, UNMARRIED, DIVORCED }

}