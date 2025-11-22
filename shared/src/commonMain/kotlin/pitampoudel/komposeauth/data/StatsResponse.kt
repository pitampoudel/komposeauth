package pitampoudel.komposeauth.data

import kotlinx.serialization.Serializable

@Serializable
data class StatsResponse(
    val totalUsers: Long
)