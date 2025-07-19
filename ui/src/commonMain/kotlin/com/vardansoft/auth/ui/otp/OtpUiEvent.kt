package com.vardansoft.auth.ui.otp

sealed interface OtpUiEvent {
    data object Verified : OtpUiEvent
}