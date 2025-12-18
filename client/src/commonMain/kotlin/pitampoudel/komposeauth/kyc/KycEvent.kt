package pitampoudel.komposeauth.kyc

import kotlinx.datetime.LocalDate
import pitampoudel.core.domain.KmpFile
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.domain.DocumentType

sealed interface KycEvent {
    data object LoadExisting : KycEvent
    // Personal detail events
    data class CountryChanged(val value: String) : KycEvent
    data class NationalityChanged(val value: String) : KycEvent
    data class FirstNameChanged(val value: String) : KycEvent
    data class MiddleNameChanged(val value: String) : KycEvent
    data class LastNameChanged(val value: String) : KycEvent
    data class DateOfBirthChanged(val value: LocalDate?) : KycEvent
    data class GenderChanged(val value: KycResponse.Gender?) : KycEvent
    // Family detail events
    data class FatherNameChanged(val value: String) : KycEvent
    data class GrandFatherNameChanged(val value: String) : KycEvent
    data class MaritalStatusChanged(val value: KycResponse.MaritalStatus?) : KycEvent

    // Address Details Events
    data class CurrentAddressCountryChanged(val value: String) : KycEvent
    data class CurrentAddressStateChanged(val value: String) : KycEvent
    data class CurrentAddressCityChanged(val value: String) : KycEvent
    data class CurrentAddressAddressLine1Changed(val value: String) : KycEvent
    data class CurrentAddressAddressLine2Changed(val value: String) : KycEvent

    data class PermanentAddressCountryChanged(val value: String) : KycEvent
    data class PermanentAddressStateChanged(val value: String) : KycEvent
    data class PermanentAddressCityChanged(val value: String) : KycEvent
    data class PermanentAddressAddressLine1Changed(val value: String) : KycEvent
    data class PermanentAddressAddressLine2Changed(val value: String) : KycEvent

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
    data object SaveAndContinue : KycEvent
    data object DismissInfoMsg : KycEvent
}