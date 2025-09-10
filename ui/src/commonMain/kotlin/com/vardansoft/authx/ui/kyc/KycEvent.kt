package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.DocumentType
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.core.domain.KmpFile
import kotlinx.datetime.LocalDate

sealed interface KycEvent {
    data object LoadExisting : KycEvent
    // Personal detail events
    data class NationalityChanged(val value: String) : KycEvent
    data class FirstNameChanged(val value: String) : KycEvent
    data class MiddleNameChanged(val value: String) : KycEvent
    data class LastNameChanged(val value: String) : KycEvent
    data class DateOfBirthChanged(val value: LocalDate?) : KycEvent
    data class GenderChanged(val value: KycResponse.Gender?) : KycEvent
    // Family detail events
    data class FatherNameChanged(val value: String) : KycEvent
    data class MotherNameChanged(val value: String) : KycEvent
    data class MaritalStatusChanged(val value: KycResponse.MaritalStatus?) : KycEvent

    // Address Details Events

    data class CurrentAddressCountryChanged(val value: String) : KycEvent
    data class CurrentAddressProvinceChanged(val value: String) : KycEvent
    data class CurrentAddressDistrictChanged(val value: String) : KycEvent
    data class CurrentAddressLocalUnitChanged(val value: String) : KycEvent
    data class CurrentAddressWardNoChanged(val value: String) : KycEvent
    data class CurrentAddressToleChanged(val value: String) : KycEvent

    data class PermanentAddressCountryChanged(val value: String) : KycEvent
    data class PermanentAddressProvinceChanged(val value: String) : KycEvent
    data class PermanentAddressDistrictChanged(val value: String) : KycEvent
    data class PermanentAddressLocalUnitChanged(val value: String) : KycEvent
    data class PermanentAddressWardNoChanged(val value: String) : KycEvent
    data class PermanentAddressToleChanged(val value: String) : KycEvent

    data class CurrentAddressSameAsPermanentChanged(val value: Boolean) : KycEvent

    // Document detail events
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
