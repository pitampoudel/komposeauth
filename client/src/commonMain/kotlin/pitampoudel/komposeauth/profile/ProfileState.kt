package pitampoudel.komposeauth.profile

import pitampoudel.core.domain.KmpFile
import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.core.data.ProfileResponse
import pitampoudel.komposeauth.core.data.UpdateProfileRequest

data class ProfileState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val profile: ProfileResponse? = null,
    val askingDeactivateConfirmation: Boolean = false,
    val editingState: EditingState = EditingState(),
    val webAuthnRegistrationOptions: String? = null
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