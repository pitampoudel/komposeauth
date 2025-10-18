package com.vardansoft.komposeauth.profile

import com.vardansoft.core.presentation.InfoMessage

sealed interface ProfileEvent {
    data class RegisterPublicKey(val publicKey: String) : ProfileEvent
    data class InfoMsgChanged(val msg: InfoMessage?) : ProfileEvent
    data object LogOut : ProfileEvent
    data class Deactivate(val confirmed: Boolean) : ProfileEvent
    data object DismissDeactivateConfirmation : ProfileEvent
    sealed interface EditEvent : ProfileEvent {
        data class GivenNameChanged(val value: String) : EditEvent
        data class FamilyNameChanged(val value: String) : EditEvent
        object Submit : EditEvent
    }
}
