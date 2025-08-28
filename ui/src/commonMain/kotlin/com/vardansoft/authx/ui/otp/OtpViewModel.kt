package com.vardansoft.authx.ui.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.authx.domain.AuthClient
import com.vardansoft.authx.domain.AuthXPreferences
import com.vardansoft.authx.domain.use_cases.ValidateOtpCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class OtpViewModel(
    val validateOtpCode: ValidateOtpCode,
    val client: AuthClient,
    val authXPreferences: AuthXPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(OtpState())
    val state = _state.asStateFlow()

    private val uiEventChannel = Channel<OtpUiEvent>()
    val uiEvents = uiEventChannel.receiveAsFlow()
    fun onEvent(event: OtpEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            when (event) {
                is OtpEvent.DismissInfoMsg -> _state.update {
                    it.copy(infoMsg = null)
                }

                is OtpEvent.CodeChanged -> _state.update {
                    it.copy(code = event.value, codeError = null)
                }

                OtpEvent.Verify -> {
                    _state.update {
                        it.copy(progress = 0.0F)
                    }

                    _state.update {
                        it.copy(codeError = validateOtpCode(state.value.code).errorMessage())
                    }

                    state.value.verifyParam()?.let { req ->
                        val res = client.verifyPhoneOtp(req)
                        when {
                            res.isFailure -> {
                                _state.update {
                                    it.copy(infoMsg = res.exceptionOrNull()?.message)
                                }
                            }

                            res.isSuccess -> {
                                val res = client.fetchUserInfo()
                                when {
                                    res.isFailure -> {
                                        _state.update {
                                            it.copy(infoMsg = res.exceptionOrNull()?.message)
                                        }
                                    }

                                    res.isSuccess -> {
                                        authXPreferences.updateUserInformation(res.getOrThrow())
                                        uiEventChannel.send(OtpUiEvent.Verified)
                                    }
                                }

                            }
                        }
                    }
                    _state.update {
                        it.copy(progress = null)
                    }

                }

                is OtpEvent.SendOtp -> {
                    val req = state.value.req ?: return@launch
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    val res = client.sendPhoneOtp(req)
                    when {
                        res.isFailure -> {
                            _state.update {
                                it.copy(
                                    infoMsg = res.exceptionOrNull()?.message,
                                    progress = null,
                                    req = null
                                )
                            }
                        }

                        res.isSuccess -> {
                            _state.update {
                                it.copy(progress = null)
                            }
                        }
                    }
                }

                is OtpEvent.SubmitPhoneNumber -> _state.update {
                    it.copy(req = event.req)
                }
            }
        }
    }
}