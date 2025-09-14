package com.vardansoft.authx.ui.login

import com.vardansoft.authx.data.Credential
import com.vardansoft.core.data.NetworkResult

sealed interface LoginEvent {
    data class Login(val credential: NetworkResult<Credential>) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
