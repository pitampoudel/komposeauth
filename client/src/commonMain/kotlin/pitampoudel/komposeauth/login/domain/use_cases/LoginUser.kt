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
    /** @return whether the user is now signed in; on failure [onError] has been told why. */
    suspend operator fun invoke(credential: Credential, onError: (InfoMessage.Error) -> Unit): Boolean {
        return when (currentPlatform()) {
            Platform.WEB -> when (val res = authClient.login(credential, ResponseType.COOKIE)) {
                is Result.Error -> false.also { onError(res.message) }

                is Result.Success -> true.also { authStateHandler.updateCurrentUser() }
            }

            else -> when (val res = authClient.login(credential)) {
                is Result.Error -> false.also { onError(res.message) }

                is Result.Success -> true.also { authPreferences.saveTokenData(tokenData = res.data) }
            }
        }
    }
}