package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.DocumentType
import com.vardansoft.core.domain.KmpFile
import kotlinx.datetime.LocalDate

sealed interface KycEvent {
    data object LoadExisting : KycEvent
    data class FullNameChanged(val value: String) : KycEvent
    data class CountryChanged(val value: String) : KycEvent
    data class DocumentTypeChanged(val value: DocumentType?) : KycEvent
    data class DocumentNumberChanged(val value: String) : KycEvent
    data class DocumentIssuedDateChanged(val value: LocalDate?) : KycEvent
    data class DocumentExpiryDateChanged(val value: LocalDate?) : KycEvent
    data class DocumentIssuedPlaceChanged(val value: String) : KycEvent
    data class DocumentFrontSelected(val file: KmpFile?) : KycEvent
    data class DocumentBackSelected(val file: KmpFile?) : KycEvent
    data class SelfieSelected(val file: KmpFile?) : KycEvent
    data object Submit : KycEvent
    data object DismissInfoMsg : KycEvent
}