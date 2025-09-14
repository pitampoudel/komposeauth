package com.vardansoft.core.domain

import com.vardansoft.core.presentation.InfoMessage
import io.ktor.http.HttpStatusCode

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    open class Error(open val message: InfoMessage.Error) : Result<Nothing>() {
        constructor(msg: String) : this(InfoMessage.Error(msg))

        data class Http(
            override val message: InfoMessage.Error,
            val httpStatusCode: HttpStatusCode
        ) : Error(message)
    }
}