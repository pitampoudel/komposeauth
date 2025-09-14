package com.vardansoft.core.data

import com.vardansoft.core.domain.KmpFile
import com.vardansoft.core.domain.Result
import com.vardansoft.core.presentation.InfoMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.contentType
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

suspend fun downloadAll(
    urls: List<String>
): Result<List<KmpFile>> {
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
        Result.Success(deferredList.awaitAll())
    }
}

suspend fun download(
    url: String
): Result<KmpFile> {
    val res = safeApiCall {
        val client = HttpClient()
        val response: HttpResponse = client.get(url)
        Result.Success(
            KmpFile(
                byteArray = response.readRawBytes(),
                mimeType = response.contentType()?.toString() ?: "application/octet-stream"
            )
        )
    }
    return res
}


suspend inline fun <reified T> HttpResponse.asResource(parse: HttpResponse.() -> T): Result<T> {
    return try {
        when (status.value) {
            in 200..299 -> Result.Success(parse())
            else -> {
                try {
                    val message = body<MessageResponse>().message
                    Result.Error.Http(InfoMessage.Error(message), this.status)
                } catch (e: JsonConvertException) {
                    val message = body<ErrorResponse>().error
                    Result.Error.Http(InfoMessage.Error(message), this.status)
                } catch (e: JsonConvertException) {
                    val message = body<DetailResponse>().detail
                    Result.Error.Http(InfoMessage.Error(message), this.status)
                } catch (e: JsonConvertException) {
                    val message = body<GoogleErrorResponse>().error.message
                    Result.Error.Http(InfoMessage.Error(message), this.status)
                } catch (e: JsonConvertException) {
                    val message = bodyAsText()
                    Result.Error.Http(InfoMessage.Error(message), this.status)
                }

            }
        }
    } catch (ex: CancellationException) {
        throw ex
    } catch (e: JsonConvertException) {
        e.printStackTrace()
        // TODO don't show technical error message to user
        Result.Error.Http(InfoMessage.Error(e.message.orEmpty()), this.status)
    } catch (e: Exception) {
        e.printStackTrace()
        // TODO don't show technical error message to user
        Result.Error.Http(InfoMessage.Error(e.message.orEmpty()), this.status)
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
        // TODO don't show technical error message to user
        Result.Error(InfoMessage.Error(ex.message.orEmpty()))
    }
}

