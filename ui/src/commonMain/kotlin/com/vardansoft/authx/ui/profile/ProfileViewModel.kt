package com.vardansoft.authx.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.domain.AuthXPreferences
import com.vardansoft.authx.ui.core.ResultUiEvent
import com.vardansoft.core.domain.Result
import com.vardansoft.core.domain.validators.ValidateNotBlank
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    val authXPreferences: AuthXPreferences,
    val client: AuthXClient,
    val validateNotBlank: ValidateNotBlank
) : ViewModel() {

    private val uiEventChannel = Channel<ResultUiEvent>()
    val uiEvents = uiEventChannel.receiveAsFlow()
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        authXPreferences.userInfoResponse.onEach { info ->
            _state.update {
                it.copy(
                    userInfo = info,
                    editingState = it.editingState.copy(
                        givenName = info?.givenName ?: it.editingState.givenName,
                        familyName = info?.familyName ?: it.editingState.familyName
                    )
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ProfileEvent) {
        viewModelScope.launch {
            when (event) {
                ProfileEvent.DismissInfoMsg -> _state.update {
                    it.copy(infoMsg = null)
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
                                authXPreferences.clear()
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

                ProfileEvent.LogOut -> authXPreferences.clear()

                is ProfileEvent.EditEvent.GivenNameChanged -> _state.update {
                    it.copy(
                        editingState = it.editingState.copy(
                            givenName = event.value,
                            givenNameError = null
                        )
                    )
                }

                is ProfileEvent.EditEvent.FamilyNameChanged -> _state.update {
                    it.copy(
                        editingState = it.editingState.copy(
                            familyName = event.value,
                            familyNameError = null
                        )
                    )
                }


                is ProfileEvent.EditEvent.Submit -> {
                    _state.update { it.copy(progress = 0.0f) }

                    val givenName = state.value.editingState.givenName
                    val familyName = state.value.editingState.familyName

                    val givenNameValidation = validateNotBlank(givenName)
                    val familyNameValidation = validateNotBlank(familyName)

                    _state.update { s ->
                        s.copy(
                            editingState = s.editingState.copy(
                                givenNameError = givenNameValidation.errorMessage(),
                                familyNameError = familyNameValidation.errorMessage()
                            )
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
