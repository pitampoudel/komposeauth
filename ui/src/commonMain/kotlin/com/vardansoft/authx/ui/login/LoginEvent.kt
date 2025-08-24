package com.vardansoft.authx.ui.login

import com.vardansoft.authx.domain.Credential

sealed interface LoginEvent {
    data class Login(val credential: Result<Credential>) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
