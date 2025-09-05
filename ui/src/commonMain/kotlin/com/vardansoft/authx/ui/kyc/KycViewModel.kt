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

                is KycEvent.FullNameChanged -> _state.update {
                    it.copy(
                        fullName = event.value,
                        fullNameError = null
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

                is KycEvent.CountryChanged -> _state.update {
                    it.copy(
                        country = event.value,
                        countryError = null
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
                        fullName = current?.fullName ?: s.fullName,
                        documentType = current?.documentType ?: s.documentType,
                        documentNumber = current?.documentNumber ?: s.documentNumber,
                        country = current?.country ?: s.country,
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
        val fullNameErr = validateNotBlank(current.fullName).errorMessage()
        val docTypeErr = validateNotNull(current.documentType).errorMessage()
        val docNumberErr = validateNotBlank(current.documentNumber).errorMessage()
        val countryErr = validateNotBlank(current.country).errorMessage()

        _state.update {
            it.copy(
                fullNameError = fullNameErr,
                documentTypeError = docTypeErr,
                documentNumberError = docNumberErr,
                countryError = countryErr
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
