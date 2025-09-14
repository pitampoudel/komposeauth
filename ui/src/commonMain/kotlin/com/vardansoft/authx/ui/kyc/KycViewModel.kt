package com.vardansoft.authx.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.domain.use_cases.ValidateNotBlank
import com.vardansoft.authx.domain.use_cases.ValidateNotNull
import com.vardansoft.core.data.NetworkResult
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

    fun onEvent(event: KycEvent) {
        viewModelScope.launch {
            when (event) {
                is KycEvent.LoadExisting -> loadExisting()
                is KycEvent.DismissInfoMsg -> _state.update {
                    it.copy(infoMsg = null)
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
                        familyInfo = it.familyInfo.copy(
                            fatherName = event.value,
                            fatherNameError = null
                        )
                    )
                }

                is KycEvent.MotherNameChanged -> _state.update {
                    it.copy(
                        familyInfo = it.familyInfo.copy(
                            motherName = event.value,
                            motherNameError = null
                        )
                    )
                }

                is KycEvent.MaritalStatusChanged -> _state.update {
                    it.copy(
                        familyInfo = it.familyInfo.copy(
                            maritalStatus = event.value,
                            maritalStatusError = null
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

                is KycEvent.Submit -> submit()
            }
        }
    }

    private suspend fun loadExisting() {
        _state.update {
            it.copy(progress = 0.0f)
        }
        val res = client.fetchMyKyc()
        when (res) {
            is NetworkResult.Error -> _state.update {
                it.copy(infoMsg = res.message)
            }

            is NetworkResult.Success -> {
                val current = res.data
                val documentFront = current?.documentInformation?.documentFrontUrl?.let {
                    download(url = it)
                }
                val documentBack = current?.documentInformation?.documentBackUrl?.let {
                    download(url = it)
                }
                val selfie = current?.documentInformation?.selfieUrl?.let {
                    download(url = it)
                }

                if (documentFront != null && documentFront is NetworkResult.Error) {
                    _state.update {
                        it.copy(infoMsg = documentFront.message)
                    }
                }
                if (documentBack != null && documentBack is NetworkResult.Error) {
                    _state.update {
                        it.copy(infoMsg = documentBack.message)
                    }
                }
                if (selfie != null && selfie is NetworkResult.Error) {
                    _state.update {
                        it.copy(infoMsg = selfie.message)
                    }
                }

                _state.update { s ->
                    s.copy(
                        existing = current,
                        personalInfo = s.personalInfo.copy(
                            nationality = current?.personalInformation?.nationality
                                ?: s.personalInfo.nationality,
                            firstName = current?.personalInformation?.firstName
                                ?: s.personalInfo.firstName,
                            middleName = current?.personalInformation?.middleName
                                ?: s.personalInfo.middleName,
                            lastName = current?.personalInformation?.lastName
                                ?: s.personalInfo.lastName,
                            dateOfBirth = current?.personalInformation?.dateOfBirth
                                ?: s.personalInfo.dateOfBirth,
                            gender = current?.personalInformation?.gender ?: s.personalInfo.gender
                        ),
                        familyInfo = s.familyInfo.copy(
                            fatherName = current?.familyInformation?.fatherName
                                ?: s.familyInfo.fatherName,
                            motherName = current?.familyInformation?.motherName
                                ?: s.familyInfo.motherName,
                            maritalStatus = current?.familyInformation?.maritalStatus
                                ?: s.familyInfo.maritalStatus
                        ),
                        currentAddress = current?.currentAddress?.let {
                            AddressState.fromData(it)
                        } ?: s.currentAddress,
                        permanentAddress = current?.permanentAddress?.let {
                            AddressState.fromData(it)
                        } ?: s.permanentAddress,
                        documentInfo = s.documentInfo.copy(
                            documentType = current?.documentInformation?.documentType
                                ?: s.documentInfo.documentType,
                            documentNumber = current?.documentInformation?.documentNumber
                                ?: s.documentInfo.documentNumber,
                            documentIssuedDate = current?.documentInformation?.documentIssuedDate
                                ?: s.documentInfo.documentIssuedDate,
                            documentExpiryDate = current?.documentInformation?.documentExpiryDate
                                ?: s.documentInfo.documentExpiryDate,
                            documentIssuedPlace = current?.documentInformation?.documentIssuedPlace
                                ?: s.documentInfo.documentIssuedPlace,
                            documentFront = (documentFront as? NetworkResult.Success)?.data
                                ?: s.documentInfo.documentFront,
                            documentBack = (documentBack as? NetworkResult.Success)?.data
                                ?: s.documentInfo.documentBack,
                            selfie = (selfie as? NetworkResult.Success)?.data
                                ?: s.documentInfo.selfie
                        )
                    )
                }
            }
        }
        _state.update {
            it.copy(progress = null)
        }
    }

    private suspend fun submit() {
        _state.update { it.copy(progress = 0.0f) }

        // Validate personal info
        val nationalityValidation = validateNotBlank(_state.value.personalInfo.nationality)
        val firstNameValidation = validateNotBlank(_state.value.personalInfo.firstName)
        val lastNameValidation = validateNotBlank(_state.value.personalInfo.lastName)
        val dateOfBirthValidation = validateNotNull(_state.value.personalInfo.dateOfBirth)
        val genderValidation = validateNotNull(_state.value.personalInfo.gender)

        // Validate family info
        val fatherNameValidation = validateNotBlank(_state.value.familyInfo.fatherName)
        val motherNameValidation = validateNotBlank(_state.value.familyInfo.motherName)
        val maritalStatusValidation = validateNotNull(_state.value.familyInfo.maritalStatus)

        // Validate current address
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


        // Validate document info
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
                personalInfo = s.personalInfo.copy(
                    nationalityError = nationalityValidation.errorMessage(),
                    firstNameError = firstNameValidation.errorMessage(),
                    lastNameError = lastNameValidation.errorMessage(),
                    dateOfBirthError = dateOfBirthValidation.errorMessage(),
                    genderError = genderValidation.errorMessage()
                ),
                familyInfo = s.familyInfo.copy(
                    fatherNameError = fatherNameValidation.errorMessage(),
                    motherNameError = motherNameValidation.errorMessage(),
                    maritalStatusError = maritalStatusValidation.errorMessage()
                ),
                permanentAddress = s.permanentAddress.copy(
                    countryError = permanentAddressCountryValidation.errorMessage(),
                    provinceError = permanentAddressProvinceValidation.errorMessage(),
                    districtError = permanentAddressDistrictValidation.errorMessage(),
                    localUnitError = permanentAddressLocalUnitValidation.errorMessage(),
                    wardNoError = permanentAddressWardNoValidation.errorMessage(),
                    toleError = permanentAddressToleValidation.errorMessage()
                ),
                currentAddress = s.currentAddress.copy(
                    countryError = if (!s.currentAddressSameAsPermanent) currentAddressCountryValidation.errorMessage() else null,
                    provinceError = if (!s.currentAddressSameAsPermanent) currentAddressProvinceValidation.errorMessage() else null,
                    districtError = if (!s.currentAddressSameAsPermanent) currentAddressDistrictValidation.errorMessage() else null,
                    localUnitError = if (!s.currentAddressSameAsPermanent) currentAddressLocalUnitValidation.errorMessage() else null,
                    wardNoError = if (!s.currentAddressSameAsPermanent) currentAddressWardNoValidation.errorMessage() else null,
                    toleError = if (!s.currentAddressSameAsPermanent) currentAddressToleValidation.errorMessage() else null
                ),
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

        if (!_state.value.containsError()) {
            val req = _state.value.updateKycRequest()
            val res = client.submitKyc(req)
            when (res) {
                is NetworkResult.Error -> _state.update { it.copy(infoMsg = res.message) }

                is NetworkResult.Success -> _state.update { it.copy(existing = res.data) }
            }
        }
        _state.update { it.copy(progress = null) }
    }
}