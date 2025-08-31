package com.vardansoft.authx.data

import kotlinx.serialization.Serializable

@Serializable
data class UpdateKycRequest(
    val fullName: String,
    val documentType: String,
    val documentNumber: String,
    val country: String,
    val documentFrontUrl: String? = null,
    val documentBackUrl: String? = null,
    val selfieUrl: String? = null,
)