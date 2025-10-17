package com.vardansoft.komposeauth.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class UserResponse @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val firstName: String,
    val lastName: String?,
    val email: String?,
    val emailVerified: Boolean,
    val photoUrl: String?,
    @Contextual
    val createdAt: Instant,
    @Contextual
    val updatedAt: Instant,
    val phoneNumber: String?,
    val phoneNumberVerified: Boolean
) {
    init {
        require(email != null || phoneNumber != null)
    }
}
