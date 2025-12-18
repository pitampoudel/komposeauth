package pitampoudel.komposeauth.organization.presentation

import pitampoudel.core.domain.KmpFile

sealed interface CreateOrganizationEvent {
    class NameChanged(val value: String) : CreateOrganizationEvent
    class EmailChanged(val value: String) : CreateOrganizationEvent
    class CountryNameCodeChanged(val value: String) : CreateOrganizationEvent
    class PhoneNumberChanged(val value: String) : CreateOrganizationEvent
    class LogoSelected(val kmpFile: KmpFile) : CreateOrganizationEvent
    class RegistrationNumberChanged(val value: String) : CreateOrganizationEvent
    class WebsiteChanged(val value: String) : CreateOrganizationEvent
    class DescriptionChanged(val value: String) : CreateOrganizationEvent
    class CountryChanged(val value: String) : CreateOrganizationEvent
    class ProvinceChanged(val value: String) : CreateOrganizationEvent
    class CityChanged(val value: String) : CreateOrganizationEvent
    class AddressLine1Changed(val value: String) : CreateOrganizationEvent
    class LoadOrganization(val orgId: String) : CreateOrganizationEvent
    class FacebookLinkChanged(val value: String) : CreateOrganizationEvent
    class ShowDeleteConfirmationDialog(val value: Boolean) : CreateOrganizationEvent
    class ShowActionMenu(val value: Boolean) : CreateOrganizationEvent
    data object DismissInfoMsg : CreateOrganizationEvent
    data object Submit : CreateOrganizationEvent
    data object Delete : CreateOrganizationEvent
}