package com.vardansoft.komposeauth.login

import com.vardansoft.core.presentation.InfoMessage
import com.vardansoft.komposeauth.data.Credential

sealed interface LoginEvent {
    data class Login(val credential: Credential) : LoginEvent
    data class ShowInfoMsg(val message: InfoMessage) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
