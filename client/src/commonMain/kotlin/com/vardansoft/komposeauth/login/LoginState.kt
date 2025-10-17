package com.vardansoft.komposeauth.login

import com.vardansoft.core.presentation.InfoMessage

data class LoginState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null
)
