package pitampoudel.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
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