package pitampoudel.komposeauth.organization.presentation

import pitampoudel.core.domain.KmpFile
import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.core.data.AddressInformation
import pitampoudel.komposeauth.core.domain.validators.ValidatePhoneNumber
import pitampoudel.komposeauth.organization.data.CreateOrUpdateOrganizationRequest
import pitampoudel.komposeauth.organization.data.OrganizationResponse

data class CreateOrganizationState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val existingOrganization: OrganizationResponse? = null,
    val name: String = "",
    val nameError: GeneralValidationError? = null,
    val email: String = "",
    val emailError: GeneralValidationError? = null,
    val phoneNumber: String = "",
    val phoneNumberError: GeneralValidationError? = null,
    val countryNameCode: String = ValidatePhoneNumber.DEFAULT_COUNTRY_NAME_CODE,
    val logoFile: KmpFile? = null,
    val logoFileError: GeneralValidationError? = null,
    val addressLine1: String = "",
    val addressLine1Error: GeneralValidationError? = null,
    val city: String = "",
    val cityError: GeneralValidationError? = null,
    val state: String = "",
    val stateError: GeneralValidationError? = null,
    val country: String = "",
    val countryError: GeneralValidationError? = null,
    val description: String = "",
    val descriptionError: GeneralValidationError? = null,
    val registrationNo: String = "",
    val registrationNoError: GeneralValidationError? = null,
    val website: String = "",
    val websiteError: GeneralValidationError? = null,
    val facebookLink: String = "",
    val facebookLinkError: GeneralValidationError? = null,
    val isShowingDeleteConfirmationDialog: Boolean = false,
    val isShowingActionMenu: Boolean = false

) {
    fun containsError(): Boolean {
        return nameError != null || emailError != null || phoneNumberError != null
                || logoFileError != null || addressLine1Error != null
                || cityError != null || stateError != null || countryError != null
                || descriptionError != null || registrationNoError != null || websiteError != null
                || facebookLinkError != null

    }

    fun createOrUpdateOrganizationRequest(): CreateOrUpdateOrganizationRequest? {
        return if (containsError()) {
            null
        } else {
            CreateOrUpdateOrganizationRequest(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                countryNameCode = countryNameCode,
                address = AddressInformation(
                    addressLine1 = addressLine1,
                    city = city,
                    state = state,
                    country = country
                ),
                description = description,
                registrationNo = registrationNo,
                website = website,
                facebookLink = facebookLink,
                logo = logoFile?.toEncodedData(),
                orgId = existingOrganization?.id
            )
        }
    }
}
