package pitampoudel.komposeauth.core.data

import kotlinx.serialization.Serializable

@Serializable
data class StatsResponse(
    val totalUsers: Long
)