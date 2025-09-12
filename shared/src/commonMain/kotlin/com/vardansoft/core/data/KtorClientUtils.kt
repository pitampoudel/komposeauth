package com.vardansoft.core.data

import com.vardansoft.authx.data.utils.safeApiCall
import com.vardansoft.core.domain.KmpFile
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.contentType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
        Result.success(deferredList.awaitAll())
    }
}

suspend fun download(
    url: String
): Result<KmpFile> {
    val res = safeApiCall {
        val client = HttpClient()
        val response: HttpResponse = client.get(url)
        Result.success(
            KmpFile(
                byteArray = response.readRawBytes(),
                mimeType = response.contentType()?.toString() ?: "application/octet-stream"
            )
        )
    }
    return res
}
