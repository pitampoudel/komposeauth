package com.vardansoft.core.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    @SerialName("message")
    val message: String
)
@Serializable
data class ErrorResponse(
    @SerialName("error")
    val error: String
)

@Serializable
data class DetailResponse(
    @SerialName("detail")
    val detail: String
)

@Serializable
data class GoogleErrorResponse(
    @SerialName("error")
    val error: Error
) {
    @Serializable
    data class Error(
        @SerialName("code")
        val code: Int,
        @SerialName("message")
        val message: String,
        @SerialName("status")
        val status: String
    )
}