package pitampoudel.core.domain

import io.ktor.http.HttpStatusCode
import pitampoudel.core.presentation.InfoMessage
import kotlin.js.JsExport

@JsExport
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