package com.vardansoft.authx.user.dto

import java.time.Instant

data class UserResponse(
    val id: String,
    val firstName: String,
    val lastName: String?,
    val email: String,
    val emailVerified: Boolean,
    val photoUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val phoneNumber: String?,
    val phoneNumberVerified: Boolean
)

