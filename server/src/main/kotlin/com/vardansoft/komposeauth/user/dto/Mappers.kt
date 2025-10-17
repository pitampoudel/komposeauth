package com.vardansoft.komposeauth.user.dto

import com.vardansoft.komposeauth.data.CreateUserRequest
import com.vardansoft.komposeauth.data.UpdateProfileRequest
import com.vardansoft.komposeauth.data.UserResponse
import com.vardansoft.komposeauth.user.entity.User
import com.vardansoft.core.data.parsePhoneNumber
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

fun CreateUserRequest.mapToEntity(passwordEncoder: PasswordEncoder): @Valid User {
    return User(
        id = ObjectId(),
        firstName = firstName,
        lastName = lastName,
        email = email,
        phoneNumber = phoneNumber?.let {
            parsePhoneNumber(null, it)
        }?.fullNumberInInternationalFormat,
        picture = picture,
        passwordHash = password?.let { passwordEncoder.encode(it) }
    )
}

fun User.update(req: UpdateProfileRequest, passwordEncoder: PasswordEncoder): @Valid User {
    return copy(
        firstName = req.givenName ?: firstName,
        lastName = req.familyName ?: lastName,
        email = req.email ?: email,
        emailVerified = if (req.email == null || req.email == email) emailVerified else false,
        passwordHash = req.password?.let {
            passwordEncoder.encode(req.password)
        } ?: passwordHash
    )
}

@OptIn(ExperimentalTime::class)
fun User.mapToResponseDto(): UserResponse {
    return UserResponse(
        id = this.id.toHexString(),
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        emailVerified = this.emailVerified,
        phoneNumber = this.phoneNumber,
        phoneNumberVerified = phoneNumberVerified,
        photoUrl = this.picture,
        createdAt = this.createdAt.toKotlinInstant(),
        updatedAt = this.updatedAt.toKotlinInstant()
    )
}