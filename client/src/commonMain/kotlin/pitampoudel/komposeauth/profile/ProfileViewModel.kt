package pitampoudel.komposeauth.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pitampoudel.core.domain.Result
import pitampoudel.core.domain.validators.ValidateNotBlank
import pitampoudel.core.presentation.ResultUiEvent
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.data.RegisterPublicKeyRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel internal constructor(
    private val authPreferences: AuthPreferences,
    private val client: AuthClient
) : ViewModel() {

    private val uiEventChannel = Channel<ResultUiEvent>()
    val uiEvents = uiEventChannel.receiveAsFlow()
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val res = client.fetchWebAuthnRegistrationOptions()
            val options = when (res) {
                is Result.Error -> {
                    _state.update {
                        it.copy(infoMsg = res.message)
                    }
                    null
                }

                is Result.Success -> res.data
            }
            _state.update {
                it.copy(webAuthnRegistrationOptions = options)
            }
        }
        authPreferences.authenticatedUserInfo.onEach { info ->
            _state.update {
                it.copy(
                    userInfo = info,
                    editingState = it.editingState.copy(
                        givenName = info?.givenName ?: it.editingState.givenName,
                        familyName = info?.familyName ?: it.editingState.familyName
                    ),
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ProfileEvent) {
        viewModelScope.launch {
            when (event) {
                is ProfileEvent.InfoMsgChanged -> {
                    _state.update {
                        it.copy(infoMsg = event.msg)
                    }
                }

                is ProfileEvent.RegisterPublicKey -> {
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    val res = client.registerPublicKey(
                        RegisterPublicKeyRequest(event.credential)
                    )
                    when (res) {
                        is Result.Error -> _state.update {
                            it.copy(infoMsg = res.message)
                        }

                        is Result.Success -> {

                        }
                    }
                    _state.update {
                        it.copy(progress = null)
                    }

                }

                is ProfileEvent.Deactivate -> {
                    if (event.confirmed) {
                        _state.update {
                            it.copy(progress = 0.0F)
                        }

                        when (val res = client.deactivate()) {
                            is Result.Error -> {
                                _state.update {
                                    it.copy(infoMsg = res.message, progress = null)
                                }
                            }

                            is Result.Success<*> -> {
                                _state.update {
                                    it.copy(progress = null, askingDeactivateConfirmation = false)
                                }
                                authPreferences.clear()
                            }
                        }


                    } else {
                        _state.update {
                            it.copy(askingDeactivateConfirmation = true)
                        }
                    }

                }

                ProfileEvent.DismissDeactivateConfirmation -> {
                    _state.update {
                        it.copy(askingDeactivateConfirmation = false)
                    }
                }

                ProfileEvent.LogOut -> authPreferences.clear()

                is ProfileEvent.EditEvent.GivenNameChanged -> _state.update {
                    it.copy(
                        editingState = it.editingState.copy(
                            givenName = event.value,
                            givenNameError = null
                        ),
                    )
                }

                is ProfileEvent.EditEvent.FamilyNameChanged -> _state.update {
                    it.copy(
                        editingState = it.editingState.copy(
                            familyName = event.value,
                            familyNameError = null
                        ),
                    )
                }


                is ProfileEvent.EditEvent.Submit -> {
                    _state.update { it.copy(progress = 0.0f) }

                    val givenName = state.value.editingState.givenName
                    val familyName = state.value.editingState.familyName

                    val givenNameValidation = ValidateNotBlank(givenName)
                    val familyNameValidation = ValidateNotBlank(familyName)

                    _state.update { s ->
                        s.copy(
                            editingState = s.editingState.copy(
                                givenNameError = givenNameValidation.error(),
                                familyNameError = familyNameValidation.error()
                            ),
                        )
                    }

                    _state.value.editingState.toRequest()?.let { req ->
                        when (val res = client.updateProfile(req)) {
                            is Result.Error -> _state.update {
                                it.copy(infoMsg = res.message)
                            }

                            is Result.Success<*> -> {
                                _state.update {
                                    it.copy(editingState = ProfileState.EditingState())
                                }
                                uiEventChannel.send(ResultUiEvent.Completed)
                            }
                        }
                    }

                    _state.update { it.copy(progress = null) }
                }
            }
        }
    }
}