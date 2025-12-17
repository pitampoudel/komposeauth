package pitampoudel.komposeauth.core.data

import kotlinx.serialization.Serializable

@Serializable
data class CountryResponse(
    val name: String,
    val demonym: String
)