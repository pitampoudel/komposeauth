package pitampoudel.komposeauth.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.domain.currentPlatform
import pitampoudel.komposeauth.login.domain.AuthClient
import pitampoudel.komposeauth.login.domain.use_cases.LoginUser

class LoginViewModel internal constructor(
    private val authClient: AuthClient,
    private val loginUser: LoginUser
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
                    loginUser(event.credential) { msg ->
                        _state.update {
                            it.copy(infoMsg = msg)
                        }
                    }
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
}

