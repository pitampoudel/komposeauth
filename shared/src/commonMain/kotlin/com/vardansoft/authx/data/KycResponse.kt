package com.vardansoft.authx.data

import kotlinx.serialization.Serializable

@Serializable
data class KycResponse(
    val id: String,
    val userId: String,
    val fullName: String,
    val documentType: DocumentType,
    val documentNumber: String,
    val country: String,
    val status: Status,
    val remarks: String? = null,
    val documentFrontUrl: String? = null,
    val documentBackUrl: String? = null,
    val selfieUrl: String? = null,
) {
    enum class Status { PENDING, APPROVED, REJECTED }

}