package pitampoudel.komposeauth.kyc.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import pitampoudel.core.data.EncodedData
import pitampoudel.core.domain.validators.ValidateAlphabeticName
import pitampoudel.core.domain.validators.ValidateDateNotInFuture
import pitampoudel.komposeauth.core.data.AddressInformation
import pitampoudel.komposeauth.kyc.domain.DocumentType

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
    val maritalStatus: KycResponse.MaritalStatus?
) {
    init {
        require(ValidateAlphabeticName.invoke(firstName).isSuccess())
        require(ValidateAlphabeticName.invoke(lastName).isSuccess())
        middleName?.let {
            require(ValidateAlphabeticName.invoke(it, allowBlank = true).isSuccess())
        }
        fatherName?.let {
            require(ValidateAlphabeticName.invoke(it, allowBlank = true).isSuccess())
        }
        grandFatherName?.let {
            require(ValidateAlphabeticName.invoke(it, allowBlank = true).isSuccess())
        }
        require(ValidateDateNotInFuture(dateOfBirth).isSuccess())
    }
}


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
) {
    init {
        when (documentType) {
            DocumentType.PASSPORT -> require(documentExpiryDate != null) {
                "Passport must have an expiry date"
            }

            DocumentType.NATIONAL_ID -> require(documentExpiryDate == null) {
                "National ID must not have an expiry date"
            }

            DocumentType.CITIZENSHIP -> require(documentExpiryDate == null) {
                "Citizenship must not have an expiry date"
            }
        }
    }
}

@Serializable
data class UpdateAddressDetailsRequest(
    val currentAddress: AddressInformation,
    val permanentAddress: AddressInformation,
)