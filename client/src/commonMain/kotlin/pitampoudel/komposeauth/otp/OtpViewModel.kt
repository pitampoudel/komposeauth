package pitampoudel.komposeauth.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pitampoudel.core.domain.Result
import pitampoudel.core.domain.now
import pitampoudel.core.presentation.ResultUiEvent
import pitampoudel.komposeauth.login.domain.AuthClient
import pitampoudel.komposeauth.login.domain.use_cases.LoginUser
import pitampoudel.komposeauth.user.domain.validators.ValidateOtpCode

class OtpViewModel internal constructor(
    private val client: AuthClient,
    private val loginUser: LoginUser
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

                is OtpEvent.Verify -> {
                    val req = state.value.req ?: return@launch
                    _state.update {
                        it.copy(progress = 0.0F)
                    }

                    _state.update {
                        it.copy(codeError = ValidateOtpCode(state.value.code).error())
                    }

                    state.value.asVerifyRequest(req.type)?.let { req ->
                        when (val res = client.verifyOtp(req)) {
                            is Result.Error -> {
                                _state.update {
                                    it.copy(infoMsg = res.message)
                                }
                            }

                            is Result.Success -> {
                                uiEventChannel.send(ResultUiEvent.Completed)
                            }
                        }
                    }
                    _state.update {
                        it.copy(progress = null)
                    }
                }

                OtpEvent.Login -> {
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    _state.update {
                        it.copy(codeError = ValidateOtpCode(state.value.code).error())
                    }
                    state.value.asLoginCredential()?.let { req ->
                        loginUser(req) { msg ->
                            _state.update {
                                it.copy(infoMsg = msg)
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
                    when (val res = client.sendOtp(req)) {
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
                                it.copy(progress = null, lastSentAt = now())
                            }
                        }
                    }
                }

                is OtpEvent.RequestChanged -> _state.update {
                    it.copy(req = event.req)
                }
            }
        }
    }
}