package pitampoudel.komposeauth.login.domain.use_cases

import pitampoudel.core.domain.Result
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.core.data.AuthStateHandler
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.core.domain.currentPlatform
import pitampoudel.komposeauth.login.domain.AuthClient
import pitampoudel.komposeauth.login.domain.AuthPreferences
import pitampoudel.komposeauth.user.data.Credential

internal class LoginUser(
    val authClient: AuthClient,
    val authStateHandler: AuthStateHandler,
    val authPreferences: AuthPreferences
) {
    suspend operator fun invoke(credential: Credential, onError: (InfoMessage.Error) -> Unit) {
        when (currentPlatform()) {
            Platform.WEB -> when (val res = authClient.login(credential, ResponseType.COOKIE)) {
                is Result.Error -> onError(res.message)

                is Result.Success -> authStateHandler.updateCurrentUser()

            }

            else -> when (val res = authClient.login(credential)) {
                is Result.Error -> onError(res.message)

                is Result.Success -> authPreferences.saveTokenData(tokenData = res.data)
            }
        }
    }
}