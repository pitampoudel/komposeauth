package com.vardansoft.authx.ui.login

import com.vardansoft.core.presentation.InfoMessage

data class LoginState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null
)
