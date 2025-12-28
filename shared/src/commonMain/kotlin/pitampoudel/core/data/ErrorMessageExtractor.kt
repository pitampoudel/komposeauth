package pitampoudel.core.data

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import pitampoudel.core.presentation.InfoMessage

suspend fun HttpResponse.catchErrorResponse(): pitampoudel.core.domain.Result.Error.Http {
    val message = runCatching {
        extractPreferredErrorMessage(this)
    }.getOrElse {
        status.description.ifBlank { "Request failed" }
    }

    return pitampoudel.core.domain.Result.Error.Http(
        InfoMessage.Error(message),
        status
    )
}


private suspend fun extractPreferredErrorMessage(response: HttpResponse): String {
    val contentType = response.headers[HttpHeaders.ContentType]

    if (contentType?.contains("application/json", ignoreCase = true) == true) {
        val json = runCatching { response.body<JsonElement>() }.getOrNull()
        val candidate = json?.collectStringFields()?.bestErrorCandidate()

        if (!candidate.isNullOrBlank()) {
            return candidate
        }
    }

    // Fallback to raw body
    val text = response.bodyAsText()
    if (text.isNotBlank()) return text

    return response.status.description.ifBlank { "Unknown error" }
}

private fun JsonElement.collectStringFields(
    parentKey: String? = null,
    result: MutableList<Pair<String, String>> = mutableListOf()
): List<Pair<String, String>> {

    when (this) {
        is JsonObject -> {
            for ((key, value) in this) {
                value.collectStringFields(key, result)
            }
        }

        is JsonArray -> {
            forEach { it.collectStringFields(parentKey, result) }
        }

        is JsonPrimitive -> {
            val string = contentOrNull
            if (string != null && !string.isBlank()) {
                result += (parentKey.orEmpty() to string)
            }
        }
    }
    return result
}

private fun List<Pair<String, String>>.bestErrorCandidate(): String? {
    return this.maxByOrNull { (key, value) ->
        score(key, value)
    }?.second
}


private fun score(key: String, value: String): Int {
    val normalizedKey = key.lowercase()

    var score = 0

    // Strong positive signals
    when {
        normalizedKey in preferredKeys -> score += 100
        normalizedKey.contains("error") -> score += 80
        normalizedKey.contains("message") -> score += 70
        normalizedKey.contains("detail") -> score += 60
        normalizedKey.contains("reason") -> score += 50
    }

    // Negative signals (metadata / technical fields)
    when {
        normalizedKey in ignoredKeys -> score -= 100
        normalizedKey.contains("time") -> score -= 40
        normalizedKey.contains("path") -> score -= 40
        normalizedKey.contains("status") -> score -= 30
        normalizedKey.contains("code") -> score -= 30
        normalizedKey.contains("trace") -> score -= 30
    }

    // Value-based heuristics
    if (value.length in 8..200) score += 10      // human-readable length
    if (value.matches(Regex("\\d+"))) score -= 20 // pure numbers are bad

    return score
}

private val preferredKeys = setOf(
    "message",
    "error",
    "detail",
    "description",
    "error_description",
    "reason"
)

private val ignoredKeys = setOf(
    "timestamp",
    "path",
    "status",
    "code",
    "traceid",
    "requestid",
    "instance"
)
