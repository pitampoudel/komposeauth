package pitampoudel.komposeauth.profile

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.data.UpdateProfileRequest
import pitampoudel.komposeauth.data.UserInfoResponse

data class ProfileState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val userInfo: UserInfoResponse? = null,
    val askingDeactivateConfirmation: Boolean = false,
    val editingState: EditingState = EditingState(),
    val webAuthnRegistrationOptions: String? = null
) {
    data class EditingState(
        val givenName: String = "",
        val givenNameError: GeneralValidationError? = null,
        val familyName: String = "",
        val familyNameError: GeneralValidationError? = null
    ) {
        fun hasError(): Boolean {
            return givenNameError != null || familyNameError != null
        }

        fun toRequest() = if (!hasError()) UpdateProfileRequest(
            givenName = givenName,
            familyName = familyName
        ) else null

    }
}