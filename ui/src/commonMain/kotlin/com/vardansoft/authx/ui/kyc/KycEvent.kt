package com.vardansoft.authx.ui.kyc

sealed interface KycEvent {
    data object LoadExisting : KycEvent
    data class FullNameChanged(val value: String) : KycEvent
    data class DocumentTypeChanged(val value: String) : KycEvent
    data class DocumentNumberChanged(val value: String) : KycEvent
    data class CountryChanged(val value: String) : KycEvent
    data class DocumentFrontUrlChanged(val value: String) : KycEvent
    data class DocumentBackUrlChanged(val value: String) : KycEvent
    data class SelfieUrlChanged(val value: String) : KycEvent
    data object Submit : KycEvent
    data object DismissInfoMsg : KycEvent
}