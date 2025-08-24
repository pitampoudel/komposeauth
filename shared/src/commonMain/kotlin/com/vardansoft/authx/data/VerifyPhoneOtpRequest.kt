package com.vardansoft.authx.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyPhoneOtpRequest(
    @SerialName("otp")
    val otp: String
)