package com.vardansoft.core.presentation

sealed interface InfoMessage {
    val text: String

    data class General(override val text: String) : InfoMessage
    data class Success(override val text: String) : InfoMessage
    data class Error(override val text: String) : InfoMessage
}

fun Throwable?.toInfoMessage(): InfoMessage {
    return InfoMessage.Error(this?.message.orEmpty())
}