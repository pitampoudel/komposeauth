package pitampoudel.komposeauth.login.presentation

import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.user.data.Credential

sealed interface LoginEvent {
    data class Login(val credential: Credential) : LoginEvent
    data class ShowInfoMsg(val message: InfoMessage) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
