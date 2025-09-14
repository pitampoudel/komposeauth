package com.vardansoft.core.data

import com.vardansoft.core.domain.KmpFile
import com.vardansoft.core.presentation.InfoMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

suspend fun downloadAll(
    urls: List<String>
): NetworkResult<List<KmpFile>> {
    return safeApiCall {
        val client = HttpClient()
        var deferredList: List<Deferred<KmpFile>> = listOf()
        coroutineScope {
            deferredList = urls.map { imageUrl ->
                async {
                    val response: HttpResponse = client.get(imageUrl)
                    KmpFile(
                        byteArray = response.readRawBytes(),
                        mimeType = response.contentType()?.toString() ?: "application/octet-stream"
                    )
                }
            }
        }
        NetworkResult.Success(deferredList.awaitAll())
    }
}

suspend fun download(
    url: String
): NetworkResult<KmpFile> {
    val res = safeApiCall {
        val client = HttpClient()
        val response: HttpResponse = client.get(url)
        NetworkResult.Success(
            KmpFile(
                byteArray = response.readRawBytes(),
                mimeType = response.contentType()?.toString() ?: "application/octet-stream"
            )
        )
    }
    return res
}


sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    open class Error(open val message: InfoMessage.Error) : NetworkResult<Nothing>() {
        constructor(msg: String) : this(InfoMessage.Error(msg))

        data class Http(
            override val message: InfoMessage.Error,
            val httpStatusCode: HttpStatusCode
        ) : Error(message)
    }
}

suspend inline fun <reified T> HttpResponse.asResource(parse: HttpResponse.() -> T): NetworkResult<T> {
    return try {
        when (status.value) {
            in 200..299 -> NetworkResult.Success(parse())
            else -> {
                try {
                    val message = body<MessageResponse>().asErrorInfoMessage()
                    NetworkResult.Error.Http(message, this.status)
                } catch (e: JsonConvertException) {
                    val message = bodyAsText()
                    NetworkResult.Error.Http(InfoMessage.Error(message), this.status)
                }

            }
        }
    } catch (ex: CancellationException) {
        throw ex
    } catch (e: JsonConvertException) {
        e.printStackTrace()
        // TODO don't show technical error message to user
        NetworkResult.Error.Http(InfoMessage.Error(e.message.orEmpty()), this.status)
    } catch (e: Exception) {
        e.printStackTrace()
        // TODO don't show technical error message to user
        NetworkResult.Error.Http(InfoMessage.Error(e.message.orEmpty()), this.status)
    }
}

suspend inline fun <reified T> safeApiCall(
    crossinline apiCall: suspend () -> NetworkResult<T>
): NetworkResult<T> {
    return try {
        apiCall.invoke()
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        ex.printStackTrace()
        // TODO don't show technical error message to user
        NetworkResult.Error(InfoMessage.Error(ex.message.orEmpty()))
    }
}

