package com.vardansoft.komposeauth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.komposeauth.ui.core.domain.AuthClient
import com.vardansoft.komposeauth.ui.core.domain.AuthPreferences
import com.vardansoft.core.domain.Result
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

    fun onEvent(event: LoginEvent) {
        viewModelScope.launch {
            when (event) {
                is LoginEvent.Login -> {
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    when {
                        event.credential is Result.Error -> _state.update {
                            it.copy(infoMsg = event.credential.message)
                        }

                        event.credential is Result.Success -> login(event.credential.data)
                    }

                    _state.update {
                        it.copy(progress = null)
                    }
                }

                LoginEvent.DismissInfoMsg -> _state.update {
                    it.copy(infoMsg = null)
                }
            }
        }
    }

    suspend fun login(cred: Credential) {
        val res = authClient.exchangeCredentialForToken(cred)
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

