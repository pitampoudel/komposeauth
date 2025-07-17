package com.vardansoft.auth.login.presentation

sealed interface LoginEvent {
    data class Login(val credential: Result<Credential>) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
