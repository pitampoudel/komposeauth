package pitampoudel.komposeauth.organization.presentation


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
import pitampoudel.core.domain.validators.ValidateNotBlank
import pitampoudel.core.domain.validators.ValidateNotNull
import pitampoudel.core.domain.validators.ValidateUrlOrBlank
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.core.presentation.ResultUiEvent
import pitampoudel.komposeauth.core.domain.validators.ValidateEmail
import pitampoudel.komposeauth.core.domain.validators.ValidateFacebookLinkOrBlank
import pitampoudel.komposeauth.core.domain.validators.ValidatePhoneNumber
import pitampoudel.komposeauth.organization.domain.OrganizationsClient
import pitampoudel.komposeauth.organization.domain.use_cases.ValidateOrganizationDescription
import pitampoudel.komposeauth.organization.domain.use_cases.ValidateOrganizationPhoneNumber
import pitampoudel.komposeauth.organization.domain.use_cases.ValidateOrganizationRegNum

class CreateOrganizationViewModel(
    val client: OrganizationsClient,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateOrganizationState())
    val state = _state.asStateFlow()
    private val uiEventChannel = Channel<ResultUiEvent>()
    val uiEvents = uiEventChannel.receiveAsFlow()
    fun onEvent(event: CreateOrganizationEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is CreateOrganizationEvent.DismissInfoMsg -> _state.update {
                    it.copy(infoMsg = null)
                }

                is CreateOrganizationEvent.ShowActionMenu -> _state.update {
                    it.copy(isShowingActionMenu = event.value)
                }

                is CreateOrganizationEvent.ShowDeleteConfirmationDialog -> _state.update {
                    it.copy(isShowingDeleteConfirmationDialog = event.value)
                }

                is CreateOrganizationEvent.LoadOrganization -> {
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    when (val res = client.get(event.orgId)) {
                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    infoMsg = res.message,
                                    progress = null
                                )
                            }
                        }

                        is Result.Success -> {
                            _state.update { createOrEditOrganizationState ->
                                createOrEditOrganizationState.copy(
                                    progress = null,
                                    existingOrganization = res.data,
                                    name = res.data.name,
                                    email = res.data.email,
                                    phoneNumber = res.data.phoneNumber?.nationalNumber?.toString()
                                        .orEmpty(),
                                    countryNameCode = res.data.phoneNumber?.countryNameCode
                                        ?: ValidatePhoneNumber.DEFAULT_COUNTRY_NAME_CODE,
                                    description = res.data.description.orEmpty(),
                                    registrationNo = res.data.registrationNo.orEmpty(),
                                    website = res.data.website.orEmpty(),
                                    addressLine1 = res.data.address.addressLine1.orEmpty(),
                                    city = res.data.address.city.orEmpty(),
                                    state = res.data.address.state.orEmpty(),
                                    country = res.data.address.country.orEmpty(),
                                    facebookLink = res.data.socialLinks.find {
                                        it.contains(
                                            "facebook.com",
                                            ignoreCase = true
                                        )
                                    }.orEmpty()
                                )
                            }

                        }
                    }
                }

                is CreateOrganizationEvent.EmailChanged -> _state.update {
                    it.copy(email = event.value, emailError = null)
                }

                is CreateOrganizationEvent.CountryNameCodeChanged -> _state.update {
                    it.copy(countryNameCode = event.value)
                }

                is CreateOrganizationEvent.PhoneNumberChanged -> _state.update {
                    it.copy(phoneNumber = event.value, phoneNumberError = null)
                }

                is CreateOrganizationEvent.NameChanged -> _state.update {
                    it.copy(name = event.value, nameError = null)
                }

                is CreateOrganizationEvent.LogoSelected -> _state.update {
                    it.copy(logoFile = event.kmpFile, logoFileError = null)
                }

                is CreateOrganizationEvent.DescriptionChanged -> _state.update {
                    it.copy(description = event.value, descriptionError = null)
                }

                is CreateOrganizationEvent.RegistrationNumberChanged -> _state.update {
                    it.copy(registrationNo = event.value, registrationNoError = null)
                }

                is CreateOrganizationEvent.WebsiteChanged -> _state.update {
                    it.copy(website = event.value, websiteError = null)
                }

                is CreateOrganizationEvent.CountryChanged -> _state.update {
                    it.copy(country = event.value, countryError = null)
                }

                is CreateOrganizationEvent.AddressLine1Changed -> _state.update {
                    it.copy(addressLine1 = event.value, addressLine1Error = null)
                }

                is CreateOrganizationEvent.CityChanged -> _state.update {
                    it.copy(city = event.value, cityError = null)
                }

                is CreateOrganizationEvent.StateChanged -> _state.update {
                    it.copy(state = event.value, stateError = null)
                }

                is CreateOrganizationEvent.FacebookLinkChanged -> _state.update {
                    it.copy(facebookLink = event.value, facebookLinkError = null)
                }

                CreateOrganizationEvent.Submit -> {
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    _state.update {
                        it.copy(logoFileError = ValidateNotNull(state.value.logoFile).error())
                    }

                    _state.update {
                        it.copy(addressLine1Error = ValidateNotBlank(state.value.addressLine1).error())
                    }
                    _state.update {
                        it.copy(cityError = ValidateNotBlank(state.value.city).error())
                    }
                    _state.update {
                        it.copy(stateError = ValidateNotBlank(state.value.state).error())
                    }
                    _state.update {
                        it.copy(countryError = ValidateNotBlank(state.value.country).error())
                    }
                    _state.update {
                        it.copy(descriptionError = ValidateOrganizationDescription(state.value.description).error())
                    }
                    _state.update {
                        it.copy(emailError = ValidateEmail(state.value.email).error())
                    }
                    _state.update {
                        it.copy(nameError = ValidateNotBlank(state.value.name).error())
                    }
                    _state.update {
                        it.copy(
                            phoneNumberError = ValidateOrganizationPhoneNumber(
                                state.value.phoneNumber,
                                state.value.countryNameCode
                            ).error()
                        )
                    }
                    _state.update {
                        it.copy(registrationNoError = ValidateOrganizationRegNum(state.value.registrationNo).error())
                    }
                    _state.update {
                        it.copy(websiteError = ValidateUrlOrBlank(state.value.website).error())
                    }
                    _state.update {
                        it.copy(facebookLinkError = ValidateFacebookLinkOrBlank(state.value.facebookLink).error())
                    }

                    state.value.createOrUpdateOrganizationRequest()?.let { req ->
                        when (val res = client.createOrUpdate(
                            request = req
                        )) {
                            is Result.Error -> {
                                _state.update {
                                    it.copy(infoMsg = res.message)
                                }
                            }

                            is Result.Success -> {
                                _state.update {
                                    CreateOrganizationState(infoMsg = InfoMessage.Success(res.data.message))
                                }
                                uiEventChannel.send(ResultUiEvent.Completed)
                            }
                        }
                    }
                    _state.update {
                        it.copy(progress = null)
                    }

                }

                CreateOrganizationEvent.Delete -> {
                    val id = state.value.existingOrganization?.id ?: return@launch
                    _state.update {
                        it.copy(progress = 0.0F)
                    }
                    when (val res = client.delete(id)) {
                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    infoMsg = res.message,
                                    progress = null
                                )
                            }
                        }

                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    progress = null
                                )
                            }
                            uiEventChannel.send(ResultUiEvent.Completed)
                        }
                    }
                }
            }
        }

    }
}
