package com.vardansoft.core.data

import com.vardansoft.core.presentation.InfoMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    @SerialName("message")
    val message: String
) {
    fun asSuccessInfoMessage() = InfoMessage.Success(message)
    fun asErrorInfoMessage() = InfoMessage.Error(message)
}