package pitampoudel.komposeauth.organization.presentation 

sealed interface OrganizationEvent {
    class Load(val orgId: String) : OrganizationEvent
    data object DismissInfoMsg : OrganizationEvent

}