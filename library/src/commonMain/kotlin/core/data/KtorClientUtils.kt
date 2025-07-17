package com.vardansoft.auth.core.data

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException

suspend inline fun <reified T> HttpResponse.asResource(parse: HttpResponse.() -> T): Result<T> {
    return try {
        when (status.value) {
            in 200..299 -> Result.success(parse())
            else -> {
                Result.failure(body())
            }
        }
    } catch (ex: CancellationException) {
        throw ex
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
        Result.failure(ex)
    }
}
