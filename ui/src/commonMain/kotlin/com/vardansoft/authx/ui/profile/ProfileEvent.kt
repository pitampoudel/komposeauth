package com.vardansoft.authx.ui.profile

sealed interface ProfileEvent {
    data object DismissInfoMsg : ProfileEvent
    data object LogOut : ProfileEvent
    data class Deactivate(val confirmed: Boolean) : ProfileEvent
    data object DismissDeactivateConfirmation : ProfileEvent
    sealed interface EditEvent : ProfileEvent {
        data class GivenNameChanged(val value: String) : EditEvent
        data class FamilyNameChanged(val value: String) : EditEvent
        object Submit : EditEvent
    }
}
