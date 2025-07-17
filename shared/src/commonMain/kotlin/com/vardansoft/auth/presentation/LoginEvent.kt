package com.vardansoft.auth.presentation

sealed interface LoginEvent {
    data class Login(val credential: Result<Credential>) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
