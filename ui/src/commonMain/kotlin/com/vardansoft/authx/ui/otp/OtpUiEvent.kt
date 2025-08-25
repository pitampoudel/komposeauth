package com.vardansoft.authx.ui.otp

internal sealed interface OtpUiEvent {
    data object Verified : OtpUiEvent
}