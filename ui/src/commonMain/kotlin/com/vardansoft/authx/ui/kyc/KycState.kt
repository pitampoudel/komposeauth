package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.UpdateKycRequest

data class KycState(
    val fullName: String = "",
    val fullNameError: String? = null,
    val documentType: String = "",
    val documentTypeError: String? = null,
    val documentNumber: String = "",
    val documentNumberError: String? = null,
    val country: String = "",
    val countryError: String? = null,
    val documentFrontUrl: String = "",
    val documentBackUrl: String = "",
    val selfieUrl: String = "",
    val progress: Float? = null,
    val infoMsg: String? = null,
    val existing: KycResponse? = null
) {
    fun containsError() = fullNameError != null || documentTypeError != null
            || documentNumberError != null || countryError != null

    fun updateKycRequest(): UpdateKycRequest {
        require(!containsError()) { "Form contains errors" }
        return UpdateKycRequest(
            fullName = fullName,
            documentType = documentType,
            documentNumber = documentNumber,
            country = country,
            documentFrontUrl = documentFrontUrl.ifBlank { null },
            documentBackUrl = documentBackUrl.ifBlank { null },
            selfieUrl = selfieUrl.ifBlank { null }
        )
    }

    val isApproved: Boolean get() = existing?.status == KycResponse.Status.APPROVED
    val isPending: Boolean get() = existing?.status == KycResponse.Status.PENDING
    val isRejected: Boolean get() = existing?.status == KycResponse.Status.REJECTED
}