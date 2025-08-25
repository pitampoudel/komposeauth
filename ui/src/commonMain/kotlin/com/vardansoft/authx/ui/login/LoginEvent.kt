package com.vardansoft.authx.ui.login

import com.vardansoft.authx.data.Credential

sealed interface LoginEvent {
    data class Login(val credential: Result<Credential>) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
