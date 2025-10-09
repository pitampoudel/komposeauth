package com.vardansoft.authx.ui.profile

sealed interface ProfileEvent {
    data object DismissInfoMsg : ProfileEvent
    data object LogOut : ProfileEvent
    data class Deactivate(val confirmed: Boolean) : ProfileEvent
    data object DismissDeactivateConfirmation : ProfileEvent
}
