package pitampoudel.komposeauth.user.data

import kotlinx.serialization.Serializable

@Serializable
data class RoleResponse(
    val role: String,
    val userCount: Long,
    /** Built-in roles cannot be removed from the catalog. */
    val builtIn: Boolean
)
