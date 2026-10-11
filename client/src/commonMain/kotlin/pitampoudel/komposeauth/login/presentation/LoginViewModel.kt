package pitampoudel.komposeauth.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.data.LoginOptionsResponse
import pitampoudel.komposeauth.core.domain.currentPlatform
import pitampoudel.komposeauth.login.domain.AuthClient
import pitampoudel.komposeauth.login.domain.use_cases.LoginUser
import pitampoudel.komposeauth.user.data.Credential
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LoginViewModel internal constructor(
    private val authClient: AuthClient,
    private val loginUser: LoginUser
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    init {
        // The passkey options in the config answer one sign-in and lapse with their own timeout, so
        // they are renewed while the screen is open instead of being fetched once.
        viewModelScope.launch {
            var config = refreshLoginConfig(reportError = true)
            while (true) {
                val wait = if (config == null) RETRY_AFTER else passkeyOptionsRenewal(config) ?: break
                delay(wait)
                config = refreshLoginConfig(reportError = false)
            }
        }
    }

    /** The config now in [state], or null when it could not be fetched. */
    private suspend fun refreshLoginConfig(reportError: Boolean): LoginOptionsResponse? {
        return when (val res = authClient.fetchLoginConfig(currentPlatform())) {
            is Result.Error -> {
                if (reportError) _state.update { it.copy(infoMsg = res.message) }
                null
            }

            is Result.Success -> res.data.also { config -> _state.update { it.copy(loginConfig = config) } }
        }
    }

    fun onEvent(event: LoginEvent) {
        viewModelScope.launch {
            when (event) {
                is LoginEvent.Login -> {
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    loginUser(event.credential) { msg ->
                        _state.update {
                            it.copy(infoMsg = msg)
                        }
                    }
                    // Spent whether or not it signed anybody in, and the next attempt needs its own.
                    if (event.credential is Credential.PublicKey) refreshLoginConfig(reportError = false)
                    _state.update {
                        it.copy(progress = null)
                    }
                }

                is LoginEvent.ShowInfoMsg -> _state.update {
                    it.copy(infoMsg = event.message)
                }

                LoginEvent.DismissInfoMsg -> _state.update {
                    it.copy(infoMsg = null)
                }
            }
        }
    }

    private companion object {
        val RETRY_AFTER = 30.seconds

        /**
         * Halfway through the options' lifetime, so options picked up just before a renewal still
         * leave the person half of it to finish signing in; null when there are none to renew.
         */
        fun passkeyOptionsRenewal(config: LoginOptionsResponse): Duration? {
            val options = config.publicKeyAuthOptionsJson ?: return null
            val timeout = runCatching {
                Json.parseToJsonElement(options).jsonObject["timeout"]?.jsonPrimitive?.longOrNull
            }.getOrNull()?.milliseconds ?: DEFAULT_PASSKEY_TIMEOUT
            return maxOf(timeout / 2, RETRY_AFTER)
        }

        val DEFAULT_PASSKEY_TIMEOUT = 300.seconds
    }
}
