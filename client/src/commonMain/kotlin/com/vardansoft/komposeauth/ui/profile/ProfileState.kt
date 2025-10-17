package com.vardansoft.komposeauth.ui.profile

import com.vardansoft.komposeauth.data.UpdateProfileRequest
import com.vardansoft.komposeauth.data.UserInfoResponse
import com.vardansoft.core.presentation.InfoMessage
import com.vardansoft.core.domain.validators.AuthValidationError

data class ProfileState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val userInfo: UserInfoResponse? = null,
    val askingDeactivateConfirmation: Boolean = false,
    val editingState: EditingState = EditingState()
) {
    data class EditingState(
        val givenName: String = "",
        val givenNameError: AuthValidationError? = null,
        val familyName: String = "",
        val familyNameError: AuthValidationError? = null
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