package com.vardansoft.komposeauth.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.komposeauth.data.KycResponse
import com.vardansoft.komposeauth.core.domain.AuthClient
import com.vardansoft.core.domain.validators.ValidateNotBlank
import com.vardansoft.core.domain.validators.ValidateNotNull
import com.vardansoft.core.domain.Result
import com.vardansoft.core.data.download
import com.vardansoft.core.domain.validators.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class KycViewModel internal constructor(
    private val client: AuthClient
) : ViewModel() {
    private val _state = MutableStateFlow(KycState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val countriesRes = client.fetchCountries()) {
                is Result.Error -> _state.update { it.copy(infoMsg = countriesRes.message) }
                is Result.Success -> _state.update { it.copy(countries = countriesRes.data) }
            }
            _state.update {
                it.copy(
                    occupations = listOf(
                        "Student",
                        "Government Employee",
                        "Private Sector Employee",
                        "Self Employed/Business Owner",
                        "Farmer",
                        "Homemaker",
                        "Retired",
                        "Unemployed",
                        "Doctor",
                        "Engineer",
                        "Lawyer",
                        "Teacher/Professor",
                        "Artist/Musician/Writer",
                        "Skilled Laborer (e.g., Carpenter, Electrician)",
                        "Unskilled Laborer",
                        "Social Worker",
                        "Religious Professional",
                        "Military Personnel",
                        "Police Officer",
                        "Firefighter",
                        "Pilot",
                        "Journalist",
                        "Athlete",
                        "Other"
                    )
                )
            }
        }
    }

    fun onEvent(event: KycEvent) {
        viewModelScope.launch {
            when (event) {
                is KycEvent.LoadExisting -> loadExisting()
                is KycEvent.DismissInfoMsg -> _state.update {
                    it.copy(infoMsg = null)
                }

                is KycEvent.CountryChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            country = event.value,
                            countryError = null
                        )
                    )
                }

                is KycEvent.NationalityChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            nationality = event.value,
                            nationalityError = null
                        )
                    )
                }

                is KycEvent.FirstNameChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            firstName = event.value,
                            firstNameError = null
                        )
                    )
                }

                is KycEvent.MiddleNameChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            middleName = event.value,
                            middleNameError = null
                        )
                    )
                }

                is KycEvent.LastNameChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            lastName = event.value,
                            lastNameError = null
                        )
                    )
                }

                is KycEvent.DateOfBirthChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            dateOfBirth = event.value,
                            dateOfBirthError = null
                        )
                    )
                }

                is KycEvent.GenderChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            gender = event.value,
                            genderError = null
                        )
                    )
                }

                is KycEvent.FatherNameChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            fatherName = event.value,
                            fatherNameError = null
                        )
                    )
                }

                is KycEvent.GrandFatherNameChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            grandFatherName = event.value,
                            grandFatherNameError = null
                        )
                    )
                }

                is KycEvent.MotherNameChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            motherName = event.value,
                            motherNameError = null
                        )
                    )
                }

                is KycEvent.GrandMotherNameChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            grandMotherName = event.value,
                            grandMotherNameError = null
                        )
                    )
                }

                is KycEvent.MaritalStatusChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            maritalStatus = event.value,
                            maritalStatusError = null
                        )
                    )
                }

                is KycEvent.OccupationChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            occupation = event.value,
                            occupationError = null
                        )
                    )
                }

                is KycEvent.PanChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            pan = event.value,
                            panError = null
                        )
                    )
                }

                is KycEvent.EmailChanged -> _state.update {
                    it.copy(
                        personalInfo = it.personalInfo.copy(
                            email = event.value,
                            emailError = null
                        )
                    )
                }

                // Current Address Events
                is KycEvent.CurrentAddressCountryChanged -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            country = event.value,
                            countryError = null
                        )
                    )
                }

                is KycEvent.CurrentAddressStateChanged -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            state = event.value,
                            stateError = null
                        )
                    )
                }

                is KycEvent.CurrentAddressCityChanged -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            city = event.value,
                            cityError = null
                        )
                    )
                }

                is KycEvent.CurrentAddressAddressLine1Changed -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            addressLine1 = event.value,
                            addressLine1Error = null
                        )
                    )
                }

                is KycEvent.CurrentAddressAddressLine2Changed -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            addressLine2 = event.value,
                            addressLine2Error = null
                        )
                    )
                }


                // Permanent Address Events
                is KycEvent.PermanentAddressCountryChanged -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            country = event.value,
                            countryError = null
                        )
                    )
                }

                is KycEvent.PermanentAddressStateChanged -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            state = event.value,
                            stateError = null
                        )
                    )
                }

                is KycEvent.PermanentAddressCityChanged -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            city = event.value,
                            cityError = null
                        )
                    )
                }

                is KycEvent.PermanentAddressAddressLine1Changed -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            addressLine1 = event.value,
                            addressLine1Error = null
                        )
                    )
                }

                is KycEvent.PermanentAddressAddressLine2Changed -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            addressLine2 = event.value,
                            addressLine2Error = null
                        )
                    )
                }


                is KycEvent.CurrentAddressSameAsPermanentChanged -> _state.update { s ->
                    val newState = s.copy(currentAddressSameAsPermanent = event.value)
                    if (event.value) {
                        newState.copy(
                            currentAddress = s.currentAddress.copy(
                                countryError = null,
                                stateError = null,
                                addressLine1Error = null,
                                addressLine2Error = null
                            )
                        )
                    } else {
                        newState
                    }
                }

                is KycEvent.DocumentTypeChanged -> _state.update {
                    it.copy(
                        documentInfo = it.documentInfo.copy(
                            documentType = event.value,
                            documentTypeError = null
                        )
                    )
                }

                is KycEvent.DocumentNumberChanged -> _state.update {
                    it.copy(
                        documentInfo = it.documentInfo.copy(
                            documentNumber = event.value,
                            documentNumberError = null
                        )
                    )
                }

                is KycEvent.DocumentIssuedDateChanged -> _state.update {
                    it.copy(
                        documentInfo = it.documentInfo.copy(
                            documentIssuedDate = event.value,
                            documentIssuedDateError = null
                        )
                    )
                }

                is KycEvent.DocumentExpiryDateChanged -> _state.update {
                    it.copy(
                        documentInfo = it.documentInfo.copy(
                            documentExpiryDate = event.value,
                            documentExpiryDateError = null
                        )
                    )
                }

                is KycEvent.DocumentIssuedPlaceChanged -> _state.update {
                    it.copy(
                        documentInfo = it.documentInfo.copy(
                            documentIssuedPlace = event.value,
                            documentIssuedPlaceError = null
                        )
                    )
                }

                is KycEvent.DocumentFrontSelected -> _state.update {
                    it.copy(
                        documentInfo = it.documentInfo.copy(
                            documentFront = event.file,
                            documentFrontError = null
                        )
                    )
                }

                is KycEvent.DocumentBackSelected -> _state.update {
                    it.copy(
                        documentInfo = it.documentInfo.copy(
                            documentBack = event.file,
                            documentBackError = null
                        )
                    )
                }

                is KycEvent.SelfieSelected -> _state.update {
                    it.copy(
                        documentInfo = it.documentInfo.copy(
                            selfie = event.file,
                            selfieError = null
                        )
                    )
                }

                is KycEvent.SaveAndContinue -> when (state.value.currentPage) {
                    1 -> submitPersonalInfo()
                    2 -> submitAddressDetails()
                    3 -> submitDocuments()
                }
            }
        }
    }

    private suspend fun loadExisting() {
        _state.update {
            it.copy(progress = 0.0f)
        }
        val res = client.fetchMyKyc()
        when (res) {
            is Result.Error -> _state.update {
                it.copy(infoMsg = res.message)
            }

            is Result.Success -> {
                val current = res.data
                refillAll(current)
            }
        }
        _state.update {
            it.copy(progress = null)
        }
    }

    suspend fun refillAll(latestRecord: KycResponse?) {
        val documentFront = latestRecord?.documentInformation?.documentFrontUrl?.let {
            download(url = it)
        }
        val documentBack = latestRecord?.documentInformation?.documentBackUrl?.let {
            download(url = it)
        }
        val selfie = latestRecord?.documentInformation?.selfieUrl?.let {
            download(url = it)
        }

        if (documentFront != null && documentFront is Result.Error) {
            _state.update {
                it.copy(infoMsg = documentFront.message)
            }
        }
        if (documentBack != null && documentBack is Result.Error) {
            _state.update {
                it.copy(infoMsg = documentBack.message)
            }
        }
        if (selfie != null && selfie is Result.Error) {
            _state.update {
                it.copy(infoMsg = selfie.message)
            }
        }

        _state.update { s ->
            s.copy(
                status = latestRecord?.status,
                personalInfo = s.personalInfo.copy(
                    country = latestRecord?.personalInformation?.country
                        ?: s.personalInfo.country,
                    nationality = latestRecord?.personalInformation?.nationality
                        ?: s.personalInfo.nationality,
                    firstName = latestRecord?.personalInformation?.firstName
                        ?: s.personalInfo.firstName,
                    middleName = latestRecord?.personalInformation?.middleName
                        ?: s.personalInfo.middleName,
                    lastName = latestRecord?.personalInformation?.lastName
                        ?: s.personalInfo.lastName,
                    dateOfBirth = latestRecord?.personalInformation?.dateOfBirth
                        ?: s.personalInfo.dateOfBirth,
                    gender = latestRecord?.personalInformation?.gender ?: s.personalInfo.gender,
                    fatherName = latestRecord?.personalInformation?.fatherName
                        ?: s.personalInfo.fatherName,
                    grandFatherName = latestRecord?.personalInformation?.grandFatherName
                        ?: s.personalInfo.grandFatherName,
                    motherName = latestRecord?.personalInformation?.motherName
                        ?: s.personalInfo.motherName,
                    grandMotherName = latestRecord?.personalInformation?.grandMotherName
                        ?: s.personalInfo.grandMotherName,
                    maritalStatus = latestRecord?.personalInformation?.maritalStatus
                        ?: s.personalInfo.maritalStatus,
                    occupation = latestRecord?.personalInformation?.occupation
                        ?: s.personalInfo.occupation,
                    pan = latestRecord?.personalInformation?.pan ?: s.personalInfo.pan,
                    email = latestRecord?.personalInformation?.email ?: s.personalInfo.email
                ),
                currentAddress = latestRecord?.currentAddress?.let {
                    AddressState.fromData(it)
                } ?: s.currentAddress,
                permanentAddress = latestRecord?.permanentAddress?.let {
                    AddressState.fromData(it)
                } ?: s.permanentAddress,
                documentInfo = s.documentInfo.copy(
                    documentType = latestRecord?.documentInformation?.documentType
                        ?: s.documentInfo.documentType,
                    documentNumber = latestRecord?.documentInformation?.documentNumber
                        ?: s.documentInfo.documentNumber,
                    documentIssuedDate = latestRecord?.documentInformation?.documentIssuedDate
                        ?: s.documentInfo.documentIssuedDate,
                    documentExpiryDate = latestRecord?.documentInformation?.documentExpiryDate
                        ?: s.documentInfo.documentExpiryDate,
                    documentIssuedPlace = latestRecord?.documentInformation?.documentIssuedPlace
                        ?: s.documentInfo.documentIssuedPlace,
                    documentFront = (documentFront as? Result.Success)?.data
                        ?: s.documentInfo.documentFront,
                    documentBack = (documentBack as? Result.Success)?.data
                        ?: s.documentInfo.documentBack,
                    selfie = (selfie as? Result.Success)?.data
                        ?: s.documentInfo.selfie
                )
            )
        }
    }

    private suspend fun submitPersonalInfo() {
        _state.update { it.copy(progress = 0.0f) }

        val countryValidation = ValidateNotBlank(_state.value.personalInfo.country)
        val nationalityValidation = ValidateNotBlank(_state.value.personalInfo.nationality)
        val firstNameValidation = ValidateNotBlank(_state.value.personalInfo.firstName)
        val lastNameValidation = ValidateNotBlank(_state.value.personalInfo.lastName)
        val dateOfBirthValidation = ValidateNotNull(_state.value.personalInfo.dateOfBirth)
        val genderValidation = ValidateNotNull(_state.value.personalInfo.gender)
        val fatherNameValidation = ValidateNotBlank(_state.value.personalInfo.fatherName)
        val grandFatherNameValidation = ValidateNotBlank(_state.value.personalInfo.grandFatherName)
        val motherNameValidation = ValidateNotBlank(_state.value.personalInfo.motherName)
        val grandMotherNameValidation = ValidateNotBlank(_state.value.personalInfo.grandMotherName)
        val maritalStatusValidation = ValidateNotNull(_state.value.personalInfo.maritalStatus)


        _state.update { s ->
            s.copy(
                personalInfo = s.personalInfo.copy(
                    countryError = countryValidation.error(),
                    nationalityError = nationalityValidation.error(),
                    firstNameError = firstNameValidation.error(),
                    lastNameError = lastNameValidation.error(),
                    dateOfBirthError = dateOfBirthValidation.error(),
                    genderError = genderValidation.error(),
                    fatherNameError = fatherNameValidation.error(),
                    grandFatherNameError = grandFatherNameValidation.error(),
                    motherNameError = motherNameValidation.error(),
                    grandMotherNameError = grandMotherNameValidation.error(),
                    maritalStatusError = maritalStatusValidation.error()
                )
            )
        }

        if (!_state.value.personalInfo.hasError()) {
            val res = client.submitKycPersonalInfo(_state.value.personalInfo.toRequest())
            when (res) {
                is Result.Error -> _state.update { it.copy(infoMsg = res.message) }
                is Result.Success -> {
                    refillAll(res.data)
                    _state.update {
                        it.copy(
                            currentPage = it.currentPage?.plus(1)
                        )
                    }
                }
            }
        }

        _state.update { it.copy(progress = null) }
    }

    private suspend fun submitAddressDetails() {
        _state.update { it.copy(progress = 0.0f) }

        val currentAddressCountryValidation = ValidateNotBlank(_state.value.currentAddress.country)
        val currentAddressStateValidation = ValidateNotBlank(_state.value.currentAddress.state)
        val currentAddressCityValidation = ValidateNotBlank(_state.value.currentAddress.city)
        val currentAddressAddressLine1Validation =
            ValidateNotBlank(_state.value.currentAddress.addressLine1)
        val currentAddressAddressLine2Validation =
            ValidateNotBlank(_state.value.currentAddress.addressLine2)

        val permanentAddressCountryValidation =
            ValidateNotBlank(_state.value.permanentAddress.country)
        val permanentAddressStateValidation = ValidateNotBlank(_state.value.permanentAddress.state)
        val permanentAddressCityValidation = ValidateNotBlank(_state.value.permanentAddress.city)
        val permanentAddressAddressLine1Validation =
            ValidateNotBlank(_state.value.permanentAddress.addressLine1)
        val permanentAddressAddressLine2Validation =
            ValidateNotBlank(_state.value.permanentAddress.addressLine2)

        _state.update { s ->
            s.copy(
                permanentAddress = s.permanentAddress.copy(
                    addressLine1Error = permanentAddressAddressLine1Validation.error(),
                    addressLine2Error = permanentAddressAddressLine2Validation.error(),
                    cityError = permanentAddressCityValidation.error(),
                    stateError = permanentAddressStateValidation.error(),
                    countryError = permanentAddressCountryValidation.error(),
                ),
                currentAddress = s.currentAddress.copy(
                    addressLine1Error = currentAddressAddressLine1Validation.error(),
                    addressLine2Error = currentAddressAddressLine2Validation.error(),
                    cityError = currentAddressCityValidation.error(),
                    stateError = currentAddressStateValidation.error(),
                    countryError = currentAddressCountryValidation.error(),
                ),
            )
        }

        if (!_state.value.hasAddressDetailsError()) {
            val res = client.submitKycAddressDetails(_state.value.updateAddressDetailsRequest())
            when (res) {
                is Result.Error -> _state.update { it.copy(infoMsg = res.message) }
                is Result.Success -> {
                    refillAll(res.data)
                    _state.update {
                        it.copy(
                            currentPage = it.currentPage?.plus(1)
                        )
                    }
                }
            }
        }

        _state.update { it.copy(progress = null) }
    }

    private suspend fun submitDocuments() {
        _state.update { it.copy(progress = 0.0f) }

        val documentTypeValidation = ValidateNotNull(_state.value.documentInfo.documentType)
        val documentNumberValidation = ValidateNotBlank(_state.value.documentInfo.documentNumber)
        val documentIssuedDateValidation =
            ValidateNotNull(_state.value.documentInfo.documentIssuedDate)
        val documentExpiryDateValidation = ValidationResult.Success
        val documentIssuedPlaceValidation =
            ValidateNotBlank(_state.value.documentInfo.documentIssuedPlace)
        val documentFrontValidation = ValidateNotNull(_state.value.documentInfo.documentFront)
        val documentBackValidation = ValidateNotNull(_state.value.documentInfo.documentBack)
        val selfieValidation = ValidateNotNull(_state.value.documentInfo.selfie)

        _state.update { s ->
            s.copy(
                documentInfo = s.documentInfo.copy(
                    documentTypeError = documentTypeValidation.error(),
                    documentNumberError = documentNumberValidation.error(),
                    documentIssuedDateError = documentIssuedDateValidation.error(),
                    documentExpiryDateError = documentExpiryDateValidation.error(),
                    documentIssuedPlaceError = documentIssuedPlaceValidation.error(),
                    documentFrontError = documentFrontValidation.error(),
                    documentBackError = documentBackValidation.error(),
                    selfieError = selfieValidation.error()
                )
            )
        }

        if (!_state.value.documentInfo.hasError()) {
            val req = _state.value.documentInfo.toRequest()
            val res = client.submitKycDocuments(req)
            when (res) {
                is Result.Error -> _state.update { it.copy(infoMsg = res.message) }
                is Result.Success -> {
                    refillAll(res.data)
                    _state.update {
                        it.copy(
                            currentPage = null
                        )
                    }
                }
            }
        }
        _state.update { it.copy(progress = null) }
    }
}