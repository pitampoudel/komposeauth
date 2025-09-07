package com.vardansoft.authx.data

import com.vardansoft.core.data.EncodedData
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class UpdateKycRequest(
    val fullName: String,
    val country: String,
    val documentType: DocumentType,
    val documentNumber: String,
    val documentIssuedDate: LocalDate,
    val documentExpiryDate: LocalDate,
    val documentIssuedPlace: String,
    val documentFront: EncodedData? = null,
    val documentBack: EncodedData? = null,
    val selfie: EncodedData? = null
)