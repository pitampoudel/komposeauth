package pitampoudel.core.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.domain.now
import kotlin.time.Instant

@Serializable
data class MessageResponse(
    @SerialName("message")
    val message: String
)

@Serializable
data class ErrorResponse(
    @SerialName("error")
    val error: String
)

@Serializable
data class DetailResponse(
    @SerialName("detail")
    val detail: String,
    @SerialName("status_code")
    val statusCode: Int? = null
)

@Serializable
data class GoogleErrorResponse(
    @SerialName("error")
    val error: Error
) {
    @Serializable
    data class Error(
        @SerialName("code")
        val code: Int,
        @SerialName("message")
        val message: String,
        @SerialName("status")
        val status: String
    )
}

@Serializable
data class PageResponse<T>(
    @SerialName("items")
    val items: List<T>,
    @SerialName("page")
    val page: Int,
    @SerialName("pageSize")
    val pageSize: Int,
    @SerialName("totalItems")
    val totalItems: Long,
    @SerialName("hasNext")
    val hasNext: Boolean
)

@Serializable
data class ErrorSnapshotResponse(
    val timestamp: Instant = now(),
    val message: String?,
    val path: String
)