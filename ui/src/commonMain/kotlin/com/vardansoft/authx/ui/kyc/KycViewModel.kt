package com.vardansoft.authx.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.domain.use_cases.ValidateNotBlank
import com.vardansoft.authx.domain.use_cases.ValidateNotNull
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
                        nationality = event.value,
                        nationalityError = null
                    )
                }

                is KycEvent.FirstNameChanged -> _state.update {
                    it.copy(
                        firstName = event.value,
                        firstNameError = null
                    )
                }

                is KycEvent.MiddleNameChanged -> _state.update {
                    it.copy(
                        middleName = event.value,
                        middleNameError = null
                    )
                }

                is KycEvent.LastNameChanged -> _state.update {
                    it.copy(
                        lastName = event.value,
                        lastNameError = null
                    )
                }

                is KycEvent.DateOfBirthChanged -> _state.update {
                    it.copy(
                        dateOfBirth = event.value,
                        dateOfBirthError = null
                    )
                }

                is KycEvent.GenderChanged -> _state.update {
                    it.copy(
                        gender = event.value,
                        genderError = null
                    )
                }

                is KycEvent.FatherNameChanged -> _state.update {
                    it.copy(
                        fatherName = event.value,
                        fatherNameError = null
                    )
                }

                is KycEvent.MotherNameChanged -> _state.update {
                    it.copy(
                        motherName = event.value,
                        motherNameError = null
                    )
                }

                is KycEvent.MaritalStatusChanged -> _state.update {
                    it.copy(
                        maritalStatus = event.value,
                        maritalStatusError = null
                    )
                }


                is KycEvent.DocumentTypeChanged -> _state.update {
                    it.copy(
                        documentType = event.value,
                        documentTypeError = null
                    )
                }

                is KycEvent.DocumentNumberChanged -> _state.update {
                    it.copy(
                        documentNumber = event.value,
                        documentNumberError = null
                    )
                }


                is KycEvent.DocumentIssuedDateChanged -> _state.update {
                    it.copy(
                        documentIssuedDate = event.value,
                        documentIssuedDateError = null
                    )
                }

                is KycEvent.DocumentExpiryDateChanged -> _state.update {
                    it.copy(
                        documentExpiryDate = event.value,
                        documentExpiryDateError = null
                    )
                }

                is KycEvent.DocumentIssuedPlaceChanged -> _state.update {
                    it.copy(
                        documentIssuedPlace = event.value,
                        documentIssuedPlaceError = null
                    )
                }

                is KycEvent.DocumentFrontSelected -> _state.update {
                    it.copy(documentFront = event.file)
                }

                is KycEvent.DocumentBackSelected -> _state.update {
                    it.copy(documentBack = event.file)
                }

                is KycEvent.SelfieSelected -> _state.update {
                    it.copy(selfie = event.file)
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
        when {
            res.isFailure -> _state.update {
                it.copy(infoMsg = res.exceptionOrNull()?.message)
            }

            res.isSuccess -> {
                val current = res.getOrNull()
                val documentFront = current?.documentFrontUrl?.let {
                    download(url = it)
                }
                val documentBack = current?.documentBackUrl?.let {
                    download(url = it)
                }
                val selfie = current?.selfieUrl?.let {
                    download(url = it)
                }

                if (documentFront != null && documentFront.isFailure) {
                    _state.update {
                        it.copy(infoMsg = documentFront.exceptionOrNull()?.message)
                    }
                }
                if (documentBack != null && documentBack.isFailure) {
                    _state.update {
                        it.copy(infoMsg = documentBack.exceptionOrNull()?.message)
                    }
                }
                if (selfie != null && selfie.isFailure) {
                    _state.update {
                        it.copy(infoMsg = selfie.exceptionOrNull()?.message)
                    }
                }

                _state.update { s ->
                    s.copy(
                        existing = current,
                        nationality = current?.nationality ?: s.nationality,
                        firstName = current?.firstName ?: s.firstName,
                        middleName = current?.middleName ?: s.middleName,
                        lastName = current?.lastName ?: s.lastName,
                        dateOfBirth = current?.dateOfBirth ?: s.dateOfBirth,
                        gender = current?.gender ?: s.gender,
                        fatherName = current?.fatherName ?: s.fatherName,
                        motherName = current?.motherName ?: s.motherName,
                        maritalStatus = current?.maritalStatus ?: s.maritalStatus,
                        documentType = current?.documentType ?: s.documentType,
                        documentNumber = current?.documentNumber ?: s.documentNumber,
                        documentIssuedDate = current?.documentIssuedDate ?: s.documentIssuedDate,
                        documentExpiryDate = current?.documentExpiryDate ?: s.documentExpiryDate,
                        documentIssuedPlace = current?.documentIssuedPlace ?: s.documentIssuedPlace,
                        documentFront = documentFront?.getOrNull() ?: s.documentFront,
                        documentBack = documentBack?.getOrNull() ?: s.documentBack,
                        selfie = selfie?.getOrNull() ?: s.selfie
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
        val current = _state.value
        val nationalityErr = validateNotBlank(current.nationality).errorMessage()
        val firstNameErr = validateNotBlank(current.firstName).errorMessage()
        val lastNameErr = validateNotBlank(current.lastName).errorMessage()
        val dateOfBirthErr = validateNotNull(current.dateOfBirth).errorMessage()
        val genderErr = validateNotNull(current.gender).errorMessage()
        val fatherNameErr = validateNotBlank(current.fatherName).errorMessage()
        val motherNameErr = validateNotBlank(current.motherName).errorMessage()
        val maritalStatusErr = validateNotNull(current.maritalStatus).errorMessage()
        val docTypeErr = validateNotNull(current.documentType).errorMessage()
        val docNumberErr = validateNotBlank(current.documentNumber).errorMessage()
        val documentIssuedDateErr = validateNotNull(current.documentIssuedDate).errorMessage()
        val documentExpiryDateErr = validateNotNull(current.documentExpiryDate).errorMessage()
        val documentIssuedPlaceErr = validateNotBlank(current.documentIssuedPlace).errorMessage()

        _state.update {
            it.copy(
                nationalityError = nationalityErr,
                firstNameError = firstNameErr,
                lastNameError = lastNameErr,
                dateOfBirthError = dateOfBirthErr,
                genderError = genderErr,
                fatherNameError = fatherNameErr,
                motherNameError = motherNameErr,
                maritalStatusError = maritalStatusErr,
                documentTypeError = docTypeErr,
                documentNumberError = docNumberErr,
                documentIssuedDateError = documentIssuedDateErr,
                documentExpiryDateError = documentExpiryDateErr,
                documentIssuedPlaceError = documentIssuedPlaceErr
            )
        }

        if (!_state.value.containsError()) {
            val req = _state.value.updateKycRequest()
            val res = client.submitKyc(req)
            when {
                res.isFailure -> _state.update { it.copy(infoMsg = res.exceptionOrNull()?.message) }
                res.isSuccess -> _state.update { it.copy(existing = res.getOrThrow()) }
            }
        }
        _state.update { it.copy(progress = null) }
    }
}
