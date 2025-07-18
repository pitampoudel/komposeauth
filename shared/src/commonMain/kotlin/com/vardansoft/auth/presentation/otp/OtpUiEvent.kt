package com.vardansoft.auth.presentation.otp

sealed interface OtpUiEvent {
    data object Verified : OtpUiEvent
}