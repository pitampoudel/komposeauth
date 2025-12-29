package pitampoudel.komposeauth.profile

import pitampoudel.core.domain.KmpFile
import pitampoudel.core.domain.Result
import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.user.data.ProfileResponse
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.organization.data.OrganizationResponse

data class ProfileState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val profile: ProfileResponse? = null,
    val askingDeactivateConfirmation: Boolean = false,
    val editingState: EditingState = EditingState(),
    val webAuthnRegistrationOptions: String? = null,
    val organizationsRes: Result<List<OrganizationResponse>>? = null
) {
    data class EditingState(
        val givenName: String = "",
        val givenNameError: GeneralValidationError? = null,
        val familyName: String = "",
        val familyNameError: GeneralValidationError? = null,
        val picture: KmpFile? = null,
        val pictureError: GeneralValidationError? = null
    ) {
        fun hasError(): Boolean {
            return givenNameError != null || familyNameError != null || pictureError != null
        }

        fun toRequest() = if (!hasError()) UpdateProfileRequest(
            givenName = givenName,
            familyName = familyName,
            picture = picture?.toEncodedData()
        ) else null

    }
}