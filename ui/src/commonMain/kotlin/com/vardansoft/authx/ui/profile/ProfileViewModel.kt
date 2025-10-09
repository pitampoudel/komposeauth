package com.vardansoft.authx.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.domain.AuthXPreferences
import com.vardansoft.core.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    val authXPreferences: AuthXPreferences,
    val client: AuthXClient
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        authXPreferences.userInfoResponse.onEach { info ->
            _state.update {
                it.copy(userInfo = info)
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ProfileEvent) {
        viewModelScope.launch {
            when (event) {
                ProfileEvent.DismissInfoMsg -> _state.update {
                    it.copy()
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

                ProfileEvent.LogOut -> authXPreferences.clear()
            }
        }
    }
}
