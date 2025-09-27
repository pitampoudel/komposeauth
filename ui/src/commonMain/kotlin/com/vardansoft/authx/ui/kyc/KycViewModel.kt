package com.vardansoft.authx.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.core.domain.validators.ValidateNotBlank
import com.vardansoft.core.domain.validators.ValidateNotNull
import com.vardansoft.core.domain.Result
import com.vardansoft.core.data.download
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class KycViewModel(
    private val client: AuthXClient,
    val validateNotBlank: ValidateNotBlank,
    val validateNotNull: ValidateNotNull
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
                it.copy(occupations = listOf(
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
                ))
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

                is KycEvent.CurrentAddressProvinceChanged -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            province = event.value,
                            provinceError = null
                        )
                    )
                }

                is KycEvent.CurrentAddressDistrictChanged -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            district = event.value,
                            districtError = null
                        )
                    )
                }

                is KycEvent.CurrentAddressLocalUnitChanged -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            localUnit = event.value,
                            localUnitError = null
                        )
                    )
                }

                is KycEvent.CurrentAddressWardNoChanged -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            wardNo = event.value,
                            wardNoError = null
                        )
                    )
                }

                is KycEvent.CurrentAddressToleChanged -> _state.update { s ->
                    s.copy(
                        currentAddress = s.currentAddress.copy(
                            tole = event.value,
                            toleError = null
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

                is KycEvent.PermanentAddressProvinceChanged -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            province = event.value,
                            provinceError = null
                        )
                    )
                }

                is KycEvent.PermanentAddressDistrictChanged -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            district = event.value,
                            districtError = null
                        )
                    )
                }

                is KycEvent.PermanentAddressLocalUnitChanged -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            localUnit = event.value,
                            localUnitError = null
                        )
                    )
                }

                is KycEvent.PermanentAddressWardNoChanged -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            wardNo = event.value,
                            wardNoError = null
                        )
                    )
                }

                is KycEvent.PermanentAddressToleChanged -> _state.update { s ->
                    s.copy(
                        permanentAddress = s.permanentAddress.copy(
                            tole = event.value,
                            toleError = null
                        )
                    )
                }

                is KycEvent.CurrentAddressSameAsPermanentChanged -> _state.update { s ->
                    val newState = s.copy(currentAddressSameAsPermanent = event.value)
                    if (event.value) {
                        newState.copy(
                            currentAddress = s.currentAddress.copy(
                                countryError = null,
                                provinceError = null,
                                districtError = null,
                                localUnitError = null,
                                wardNoError = null,
                                toleError = null
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

        val countryValidation = validateNotBlank(_state.value.personalInfo.country)
        val nationalityValidation = validateNotBlank(_state.value.personalInfo.nationality)
        val firstNameValidation = validateNotBlank(_state.value.personalInfo.firstName)
        val lastNameValidation = validateNotBlank(_state.value.personalInfo.lastName)
        val dateOfBirthValidation = validateNotNull(_state.value.personalInfo.dateOfBirth)
        val genderValidation = validateNotNull(_state.value.personalInfo.gender)
        val fatherNameValidation = validateNotBlank(_state.value.personalInfo.fatherName)
        val grandFatherNameValidation = validateNotBlank(_state.value.personalInfo.grandFatherName)
        val motherNameValidation = validateNotBlank(_state.value.personalInfo.motherName)
        val grandMotherNameValidation = validateNotBlank(_state.value.personalInfo.grandMotherName)
        val maritalStatusValidation = validateNotNull(_state.value.personalInfo.maritalStatus)


        _state.update { s ->
            s.copy(
                personalInfo = s.personalInfo.copy(
                    countryError = countryValidation.errorMessage(),
                    nationalityError = nationalityValidation.errorMessage(),
                    firstNameError = firstNameValidation.errorMessage(),
                    lastNameError = lastNameValidation.errorMessage(),
                    dateOfBirthError = dateOfBirthValidation.errorMessage(),
                    genderError = genderValidation.errorMessage(),
                    fatherNameError = fatherNameValidation.errorMessage(),
                    grandFatherNameError = grandFatherNameValidation.errorMessage(),
                    motherNameError = motherNameValidation.errorMessage(),
                    grandMotherNameError = grandMotherNameValidation.errorMessage(),
                    maritalStatusError = maritalStatusValidation.errorMessage()
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
                            currentPage = it.currentPage + 1
                        )
                    }
                }
            }
        }

        _state.update { it.copy(progress = null) }
    }

    private suspend fun submitAddressDetails() {
        _state.update { it.copy(progress = 0.0f) }

        val currentAddressCountryValidation = validateNotBlank(_state.value.currentAddress.country)
        val currentAddressProvinceValidation =
            validateNotBlank(_state.value.currentAddress.province)
        val currentAddressDistrictValidation =
            validateNotBlank(_state.value.currentAddress.district)
        val currentAddressLocalUnitValidation =
            validateNotBlank(_state.value.currentAddress.localUnit)
        val currentAddressWardNoValidation = validateNotBlank(_state.value.currentAddress.wardNo)
        val currentAddressToleValidation = validateNotBlank(_state.value.currentAddress.tole)

        val permanentAddressCountryValidation =
            validateNotBlank(_state.value.permanentAddress.country)
        val permanentAddressProvinceValidation =
            validateNotBlank(_state.value.permanentAddress.province)

        val permanentAddressDistrictValidation =
            validateNotBlank(_state.value.permanentAddress.district)

        val permanentAddressLocalUnitValidation =
            validateNotBlank(_state.value.permanentAddress.localUnit)

        val permanentAddressWardNoValidation =
            validateNotBlank(_state.value.permanentAddress.wardNo)
        val permanentAddressToleValidation = validateNotBlank(_state.value.permanentAddress.tole)

        _state.update { s ->
            s.copy(
                permanentAddress = s.permanentAddress.copy(
                    countryError = permanentAddressCountryValidation.errorMessage(),
                    provinceError = permanentAddressProvinceValidation.errorMessage(),
                    districtError = permanentAddressDistrictValidation.errorMessage(),
                    localUnitError = permanentAddressLocalUnitValidation.errorMessage(),
                    wardNoError = permanentAddressWardNoValidation.errorMessage(),
                    toleError = permanentAddressToleValidation.errorMessage()
                ),
                currentAddress = s.currentAddress.copy(
                    countryError = currentAddressCountryValidation.errorMessage(),
                    provinceError = currentAddressProvinceValidation.errorMessage(),
                    districtError = currentAddressDistrictValidation.errorMessage(),
                    localUnitError = currentAddressLocalUnitValidation.errorMessage(),
                    wardNoError = currentAddressWardNoValidation.errorMessage(),
                    toleError = currentAddressToleValidation.errorMessage()
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
                            currentPage = it.currentPage + 1
                        )
                    }
                }
            }
        }

        _state.update { it.copy(progress = null) }
    }

    private suspend fun submitDocuments() {
        _state.update { it.copy(progress = 0.0f) }

        val documentTypeValidation = validateNotNull(_state.value.documentInfo.documentType)
        val documentNumberValidation = validateNotBlank(_state.value.documentInfo.documentNumber)
        val documentIssuedDateValidation =
            validateNotNull(_state.value.documentInfo.documentIssuedDate)
        val documentExpiryDateValidation =
            validateNotNull(_state.value.documentInfo.documentExpiryDate)
        val documentIssuedPlaceValidation =
            validateNotBlank(_state.value.documentInfo.documentIssuedPlace)
        val documentFrontValidation = validateNotNull(_state.value.documentInfo.documentFront)
        val documentBackValidation = validateNotNull(_state.value.documentInfo.documentBack)
        val selfieValidation = validateNotNull(_state.value.documentInfo.selfie)

        _state.update { s ->
            s.copy(
                documentInfo = s.documentInfo.copy(
                    documentTypeError = documentTypeValidation.errorMessage(),
                    documentNumberError = documentNumberValidation.errorMessage(),
                    documentIssuedDateError = documentIssuedDateValidation.errorMessage(),
                    documentExpiryDateError = documentExpiryDateValidation.errorMessage(),
                    documentIssuedPlaceError = documentIssuedPlaceValidation.errorMessage(),
                    documentFrontError = documentFrontValidation.errorMessage(),
                    documentBackError = documentBackValidation.errorMessage(),
                    selfieError = selfieValidation.errorMessage()
                )
            )
        }

        if (!_state.value.documentInfo.hasError()) {
            val req = _state.value.documentInfo.toRequest()
            val res = client.submitKycDocuments(req)
            when (res) {
                is Result.Error -> _state.update { it.copy(infoMsg = res.message) }
                is Result.Success -> refillAll(res.data)
            }
        }
        _state.update { it.copy(progress = null) }
    }
}