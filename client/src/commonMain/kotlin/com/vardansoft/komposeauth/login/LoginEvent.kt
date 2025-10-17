package com.vardansoft.komposeauth.login

import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.core.domain.Result

sealed interface LoginEvent {
    data class Login(val credential: Result<Credential>) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
