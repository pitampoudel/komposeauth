package pitampoudel.komposeauth.organization.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.data.EncodedData
import pitampoudel.core.domain.validators.ValidateNotBlank
import pitampoudel.core.domain.validators.ValidateUrlOrBlank
import pitampoudel.core.domain.validators.ValidationResult
import pitampoudel.komposeauth.core.domain.use_cases.ValidateEmail
import pitampoudel.komposeauth.core.domain.use_cases.ValidateFacebookLinkOrBlank
import pitampoudel.komposeauth.organization.domain.use_cases.ValidateOrganizationDescription
import pitampoudel.komposeauth.organization.domain.use_cases.ValidateOrganizationPhoneNumber
import pitampoudel.komposeauth.organization.domain.use_cases.ValidateOrganizationRegNum

@Serializable
data class CreateOrUpdateOrganizationRequest(
    @SerialName("address")
    val address: String,
    @SerialName("countryNameCode")
    val countryNameCode: String,
    @SerialName("description")
    val description: String,
    @SerialName("email")
    val email: String,
    @SerialName("name")
    val name: String,
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("registrationNo")
    val registrationNo: String,
    @SerialName("website")
    val website: String,
    @SerialName("facebookLink")
    val facebookLink: String,
    @SerialName("logo")
    val logo: EncodedData?,
    @SerialName("orgId")
    val orgId: String?
) {
    init {
        require(ValidateNotBlank(countryNameCode).isSuccess())
        require(ValidateOrganizationDescription(description).isSuccess())
        require(ValidateEmail(email).isSuccess())
        require(ValidateNotBlank(name).isSuccess())
        require(
            ValidateOrganizationPhoneNumber(
                phoneNumber,
                countryNameCode
            ).isSuccess()
        )
        require(ValidateOrganizationRegNum(registrationNo).isSuccess())
        require(ValidateUrlOrBlank(website).isSuccess())
        require(ValidateFacebookLinkOrBlank(facebookLink) is ValidationResult.Success)
    }
}
