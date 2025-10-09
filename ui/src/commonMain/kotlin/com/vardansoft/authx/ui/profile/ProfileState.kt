package com.vardansoft.authx.ui.profile

import com.vardansoft.authx.data.UpdateProfileRequest
import com.vardansoft.authx.data.UserInfoResponse
import com.vardansoft.core.presentation.InfoMessage

data class ProfileState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val userInfo: UserInfoResponse? = null,
    val askingDeactivateConfirmation: Boolean = false,
    val editingState: EditingState = EditingState()
) {
    data class EditingState(
        val givenName: String = "",
        val givenNameError: String? = null,
        val familyName: String = "",
        val familyNameError: String? = null
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