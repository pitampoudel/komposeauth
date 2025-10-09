package com.vardansoft.authx.ui.profile

import com.vardansoft.authx.data.UserInfoResponse
import com.vardansoft.core.presentation.InfoMessage

data class ProfileState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val userInfo: UserInfoResponse? = null,
    val askingDeactivateConfirmation: Boolean = false
)