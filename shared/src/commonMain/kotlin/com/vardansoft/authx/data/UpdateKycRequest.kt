package com.vardansoft.authx.data

import com.vardansoft.core.data.EncodedData
import kotlinx.serialization.Serializable

@Serializable
data class UpdateKycRequest(
    val fullName: String,
    val documentType: String,
    val documentNumber: String,
    val country: String,
    val documentFront: EncodedData? = null,
    val documentBack: EncodedData? = null,
    val selfie: EncodedData? = null
)