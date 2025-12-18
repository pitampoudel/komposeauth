package pitampoudel.komposeauth.core.data

import kotlinx.serialization.Serializable

@Serializable
data class AddressInformation(
    val country: String?,
    val state: String?,
    val city: String?,
    val addressLine1: String?,
    val addressLine2: String? = null,
)
