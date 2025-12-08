package pitampoudel.core.data

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
import kotlinx.coroutines.supervisorScope
import pitampoudel.core.domain.KmpFile
import pitampoudel.core.domain.Result
import pitampoudel.core.presentation.InfoMessage
import kotlin.coroutines.cancellation.CancellationException

suspend fun HttpClient.downloadAll(
    urls: List<String>
): Result<List<KmpFile>> {
    return safeApiCall {
        var deferredList: List<Deferred<KmpFile>> = listOf()
        supervisorScope {
            deferredList = urls.map { imageUrl ->
                async {
                    val response: HttpResponse = get(imageUrl)
                    KmpFile(
                        byteArray = response.readRawBytes(),
                        mimeType = response.contentType()?.toString() ?: "application/octet-stream",
                        name = imageUrl.substringAfterLast("/", missingDelimiterValue = "file")
                    )
                }
            }
        }
        Result.Success(deferredList.awaitAll())
    }
}

suspend fun HttpClient.download(url: String) = safeApiCall {
    val response = get(url)
    Result.Success(
        KmpFile(
            byteArray = response.readRawBytes(),
            mimeType = response.contentType()?.toString() ?: "application/octet-stream",
            name = url.substringAfterLast("/")
        )
    )
}

suspend fun HttpResponse.catchErrorResponse(): Result.Error.Http {
    val rawText = bodyAsText()
    val message = runCatching { body<MessageResponse>().message }.getOrNull()
        ?: runCatching { body<ErrorResponse>().error }.getOrNull()
        ?: runCatching { body<DetailResponse>().detail }.getOrNull()
        ?: runCatching { body<GoogleErrorResponse>().error.message }.getOrNull()
        ?: rawText
    return Result.Error.Http(InfoMessage.Error(message), status)
}

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

