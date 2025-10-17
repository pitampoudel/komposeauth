package com.vardansoft.komposeauth.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.komposeauth.core.domain.AuthClient
import com.vardansoft.komposeauth.core.domain.AuthPreferences
import com.vardansoft.komposeauth.domain.use_cases.ValidateOtpCode
import com.vardansoft.core.presentation.ResultUiEvent
import com.vardansoft.core.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtpViewModel internal constructor(
    private val client: AuthClient,
    private val authPreferences: AuthPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(OtpState())
    val state = _state.asStateFlow()

    private val uiEventChannel = Channel<ResultUiEvent>()
    val uiEvents = uiEventChannel.receiveAsFlow()
    fun onEvent(event: OtpEvent) {
        viewModelScope.launch(Dispatchers.Default) {
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
                        it.copy(codeError = ValidateOtpCode(state.value.code).error())
                    }

                    state.value.verifyParam()?.let { req ->
                        when (val res = client.verifyPhoneOtp(req)) {
                            is Result.Error -> {
                                _state.update {
                                    it.copy(infoMsg = res.message)
                                }
                            }

                            is Result.Success -> {
                                when (val res = client.fetchUserInfo()) {
                                    is Result.Error -> {
                                        _state.update {
                                            it.copy(infoMsg = res.message)
                                        }
                                    }

                                    is Result.Success -> {
                                        authPreferences.updateUserInformation(res.data)
                                        uiEventChannel.send(ResultUiEvent.Completed)
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
                    when (res) {
                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    infoMsg = res.message,
                                    progress = null,
                                    req = null
                                )
                            }
                        }

                        is Result.Success -> {
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