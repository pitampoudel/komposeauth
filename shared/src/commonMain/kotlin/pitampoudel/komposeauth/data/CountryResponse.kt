package pitampoudel.komposeauth.data

import kotlinx.serialization.Serializable

@Serializable
data class CountryResponse(
    val name: String,
    val demonym: String
)