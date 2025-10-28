package pitampoudel.komposeauth.login

import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.data.LoginOptions

data class LoginState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val loginConfig: LoginOptions? = null
)
