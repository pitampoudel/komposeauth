package com.vardansoft.authx.ui.login

import com.vardansoft.authx.data.Credential
import com.vardansoft.core.domain.Result

sealed interface LoginEvent {
    data class Login(val credential: Result<Credential>) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
