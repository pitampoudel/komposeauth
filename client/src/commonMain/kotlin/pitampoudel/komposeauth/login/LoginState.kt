package pitampoudel.komposeauth.login

import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.data.LoginOptionsResponse

data class LoginState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val loginConfig: LoginOptionsResponse? = null
)
