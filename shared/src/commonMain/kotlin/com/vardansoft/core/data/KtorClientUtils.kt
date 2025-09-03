package com.vardansoft.core.data

import com.vardansoft.authx.data.utils.safeApiCall
import com.vardansoft.core.domain.KmpFile
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
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
                    KmpFile(client.get(imageUrl).readRawBytes())
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
        Result.success(
            KmpFile(
                HttpClient().get(url).readRawBytes()
            )
        )
    }
    return res
}