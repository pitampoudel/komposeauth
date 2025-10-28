package pitampoudel.komposeauth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.domain.currentPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel internal constructor(
    private val authClient: AuthClient,
    private val authPreferences: AuthPreferences
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
        val res = authClient.exchangeCredentialForToken(event.credential)
        when (res) {
            is Result.Error -> _state.update {
                it.copy(infoMsg = res.message)
            }

            is Result.Success -> {
                val userInfoRes = authClient.fetchUserInfo(res.data.accessToken)
                when (userInfoRes) {
                    is Result.Error -> _state.update {
                        it.copy(infoMsg = userInfoRes.message)
                    }

                    is Result.Success -> {
                        authPreferences.saveLoggedInDetails(
                            token = res.data,
                            userInfoResponse = userInfoRes.data
                        )
                    }
                }

            }
        }

    }
}

