package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.UpdateKycRequest
import com.vardansoft.core.domain.KmpFile

data class KycState(
    val fullName: String = "",
    val fullNameError: String? = null,
    val documentType: String = "",
    val documentTypeError: String? = null,
    val documentNumber: String = "",
    val documentNumberError: String? = null,
    val country: String = "",
    val countryError: String? = null,
    val documentFront: KmpFile? = null,
    val documentBack: KmpFile? = null,
    val selfie: KmpFile? = null,
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
            documentFront = documentFront?.toEncodedData(),
            documentBack = documentBack?.toEncodedData(),
            selfie = selfie?.toEncodedData()
        )
    }

    val isApproved: Boolean get() = existing?.status == KycResponse.Status.APPROVED
    val isPending: Boolean get() = existing?.status == KycResponse.Status.PENDING
    val isRejected: Boolean get() = existing?.status == KycResponse.Status.REJECTED
}