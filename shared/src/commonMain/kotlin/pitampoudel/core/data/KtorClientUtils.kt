package pitampoudel.core.data

import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.JsonConvertException
import pitampoudel.core.domain.Result
import pitampoudel.core.presentation.InfoMessage
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <reified T> HttpResponse.asResource(parse: HttpResponse.() -> T): Result<T> {
    return when (status.value) {
        in 200..299 -> try {
            Result.Success(parse())
        } catch (e: JsonConvertException) {
            e.printStackTrace()
            catchErrorResponse()
        }
        else -> catchErrorResponse()
    }

}

suspend inline fun <reified T> safeApiCall(
    crossinline apiCall: suspend () -> Result<T>
): Result<T> {
    return try {
        apiCall.invoke()
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        ex.printStackTrace()
        Result.Error(InfoMessage.Error(ex.message.orEmpty()))
    }
}

