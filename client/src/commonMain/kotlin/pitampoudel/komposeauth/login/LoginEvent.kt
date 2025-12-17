package pitampoudel.komposeauth.login

import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.core.data.Credential

sealed interface LoginEvent {
    data class Login(val credential: Credential) : LoginEvent
    data class ShowInfoMsg(val message: InfoMessage) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
