package com.vardansoft.komposeauth.login

import com.vardansoft.core.presentation.InfoMessage
import com.vardansoft.komposeauth.data.LoginOptions

data class LoginState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val loginConfig: LoginOptions? = null
)
