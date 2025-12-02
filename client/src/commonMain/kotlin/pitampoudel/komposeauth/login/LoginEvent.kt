package pitampoudel.komposeauth.login

import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.ResponseType

sealed interface LoginEvent {
    data class Login(val credential: Credential, val type: ResponseType) : LoginEvent
    data class ShowInfoMsg(val message: InfoMessage) : LoginEvent
    data object DismissInfoMsg : LoginEvent
}
