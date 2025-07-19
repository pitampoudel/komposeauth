package com.vardansoft.auth.presentation

import com.vardansoft.auth.com.vardansoft.auth.domain.Credential

sealed interface LoginEvent {
    data class Login(val credential: Result<Credential>) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
