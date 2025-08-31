package com.vardansoft.authx.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.domain.AuthXPreferences
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
                        event.credential.isFailure -> _state.update {
                            it.copy(infoMsg = event.credential.exceptionOrNull()?.message)
                        }

                        event.credential.isSuccess -> login(event.credential.getOrThrow())
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
        when {
            res.isFailure -> _state.update {
                it.copy(infoMsg = res.exceptionOrNull()?.message)
            }

            res.isSuccess -> {
                val userInfoRes = authXClient.fetchUserInfo(res.getOrThrow().accessToken)
                when {
                    userInfoRes.isFailure -> _state.update {
                        it.copy(infoMsg = userInfoRes.exceptionOrNull()?.message)
                    }

                    userInfoRes.isSuccess -> {
                        authXPreferences.saveLoggedInDetails(
                            token = res.getOrThrow(),
                            userInfoResponse = userInfoRes.getOrThrow()
                        )
                    }
                }

            }
        }

    }
}

