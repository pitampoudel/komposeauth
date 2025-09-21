package com.vardansoft.authx.data

import kotlin.time.ExperimentalTime
import kotlin.time.Instant


data class UserResponse @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val firstName: String,
    val lastName: String?,
    val email: String?,
    val emailVerified: Boolean,
    val photoUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val phoneNumber: String?,
    val phoneNumberVerified: Boolean
) {
    init {
        require(email != null || phoneNumber != null)
    }
}

