package pitampoudel.komposeauth.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.login.domain.AuthClient
import pitampoudel.komposeauth.login.domain.AuthPreferences
import pitampoudel.komposeauth.core.data.AuthStateHandler
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.core.domain.currentPlatform

class LoginViewModel internal constructor(
    private val authClient: AuthClient,
    private val authPreferences: AuthPreferences,
    private val authStateHandler: AuthStateHandler
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val res = authClient.fetchLoginConfig(currentPlatform())
            val config = when (res) {
                is Result.Error -> {
                    _state.update {
                        it.copy(infoMsg = res.message)
                    }
                    null
                }

                is Result.Success -> res.data
            }
            _state.update {
                it.copy(loginConfig = config)
            }
        }
    }

    fun onEvent(event: LoginEvent) {
        viewModelScope.launch {
            when (event) {
                is LoginEvent.Login -> {
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    login(event)
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

    suspend fun login(event: LoginEvent.Login) {
        when (currentPlatform()) {
            Platform.WEB -> when (
                val res = authClient.login(event.credential, ResponseType.COOKIE)
            ) {
                is Result.Error -> _state.update {
                    it.copy(infoMsg = res.message)
                }

                is Result.Success -> {
                    authStateHandler.updateCurrentUser()
                }
            }

            else -> when (val res = authClient.login(event.credential)) {
                is Result.Error -> _state.update {
                    it.copy(infoMsg = res.message)
                }

                is Result.Success -> {
                    authPreferences.saveTokenData(
                        tokenData = res.data
                    )
                }
            }
        }


    }
}

