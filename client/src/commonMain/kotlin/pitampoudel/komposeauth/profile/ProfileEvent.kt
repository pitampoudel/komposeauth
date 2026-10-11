package pitampoudel.komposeauth.profile

import pitampoudel.core.domain.KmpFile
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.login.presentation.KmpCredentialManager

sealed interface ProfileEvent {
    /**
     * Adds a passkey to the signed-in account: fetches the options, has [credentialManager] create
     * the passkey, and registers it. Offer it where [KmpCredentialManager.canCreatePasskeys].
     */
    data class AddPasskey(val credentialManager: KmpCredentialManager) : ProfileEvent
    data class InfoMsgChanged(val msg: InfoMessage?) : ProfileEvent
    data object LogOut : ProfileEvent
    sealed interface EditEvent : ProfileEvent {
        data class GivenNameChanged(val value: String) : EditEvent
        data class FamilyNameChanged(val value: String) : EditEvent
        data class PhotoChanged(val value: KmpFile?) : EditEvent
        object Submit : EditEvent
    }
}
