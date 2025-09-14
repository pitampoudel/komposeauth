package com.vardansoft.authx.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class UserInfoResponse @OptIn(ExperimentalTime::class) constructor(
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String?,
    @SerialName("email")
    val email: String,
    @SerialName("phoneNumber")
    val phoneNumber: String?,
    @SerialName("emailVerified")
    val emailVerified: Boolean,
    @SerialName("phoneNumberVerified")
    val phoneNumberVerified: Boolean,
    @SerialName("kycVerified")
    val kycVerified: Boolean = false,
    @SerialName("picture")
    val picture: String?,
    @SerialName("id")
    val id: String,
    @SerialName("createdAt")
    @Contextual val createdAt: Instant,
    @SerialName("updatedAt")
    @Contextual val updatedAt: Instant,
    @SerialName("socialLinks")
    val socialLinks: List<String>
) {
    fun fullName() = "$firstName $lastName"
}