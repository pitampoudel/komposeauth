package com.vardansoft.authx.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.domain.AuthXPreferences
import com.vardansoft.core.data.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    val authXClient: AuthXClient,
    val authXPreferences: AuthXPreferences
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
                        event.credential is NetworkResult.Error -> _state.update {
                            it.copy(infoMsg = event.credential.message)
                        }

                        event.credential is NetworkResult.Success -> login(event.credential.data)
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
        val res = authXClient.exchangeCredentialForToken(cred)
        when (res) {
            is NetworkResult.Error -> _state.update {
                it.copy(infoMsg = res.message)
            }

            is NetworkResult.Success -> {
                val userInfoRes = authXClient.fetchUserInfo(res.data.accessToken)
                when (userInfoRes) {
                    is NetworkResult.Error -> _state.update {
                        it.copy(infoMsg = userInfoRes.message)
                    }

                    is NetworkResult.Success -> {
                        authXPreferences.saveLoggedInDetails(
                            token = res.data,
                            userInfoResponse = userInfoRes.data
                        )
                    }
                }

            }
        }

    }
}

