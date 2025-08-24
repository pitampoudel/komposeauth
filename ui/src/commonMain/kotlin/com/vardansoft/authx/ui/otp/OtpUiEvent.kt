package com.vardansoft.authx.ui.otp

sealed interface OtpUiEvent {
    data object Verified : OtpUiEvent
}