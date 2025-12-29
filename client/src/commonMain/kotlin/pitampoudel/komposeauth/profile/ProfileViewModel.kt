package pitampoudel.komposeauth.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pitampoudel.core.domain.Result
import pitampoudel.core.domain.validators.ValidateNotBlank
import pitampoudel.core.presentation.ResultUiEvent
import pitampoudel.komposeauth.core.data.AuthStateHandler
import pitampoudel.komposeauth.login.domain.AuthClient
import pitampoudel.komposeauth.user.data.ProfileResponse
import pitampoudel.komposeauth.core.data.RegisterPublicKeyRequest
import pitampoudel.komposeauth.organization.domain.OrganizationsClient

class ProfileViewModel internal constructor(
    private val authStateHandler: AuthStateHandler,
    private val client: AuthClient,
    val orgClient: OrganizationsClient
) : ViewModel() {

    private val uiEventChannel = Channel<ResultUiEvent>()
    val uiEvents = uiEventChannel.receiveAsFlow()
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val options = when (val res = client.fetchWebAuthnRegistrationOptions()) {
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
        viewModelScope.launch {
            _state.update {
                it.copy(organizationsRes = orgClient.get())
            }
        }
        viewModelScope.launch {
            _state.update {
                it.copy(progress = 0.0F)
            }
            when (val res = client.fetchUserInfo()) {
                is Result.Error -> {
                    _state.update {
                        it.copy(progress = null, infoMsg = res.message)
                    }
                }

                is Result.Success<ProfileResponse> -> _state.update {
                    it.copy(
                        progress = null,
                        profile = res.data,
                        editingState = it.editingState.copy(
                            givenName = res.data.givenName.orEmpty(),
                            familyName = res.data.familyName ?: it.editingState.familyName
                        ),
                    )
                }
            }
        }
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
                                authStateHandler.logout()
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

                ProfileEvent.LogOut -> authStateHandler.logout()

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

                is ProfileEvent.EditEvent.PhotoChanged -> _state.update {
                    it.copy(
                        editingState = it.editingState.copy(
                            picture = event.value,
                            pictureError = null
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

                            is Result.Success -> {
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