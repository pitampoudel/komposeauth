package com.vardansoft.authx.user.dto

import com.vardansoft.authx.data.CreateUserRequest
import com.vardansoft.authx.data.UpdateProfileRequest
import com.vardansoft.authx.data.UserResponse
import com.vardansoft.authx.user.entity.User
import com.vardansoft.core.data.parsePhoneNumber
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

fun CreateUserRequest.mapToEntity(passwordEncoder: PasswordEncoder): @Valid User {
    return User(
        id = id?.let { ObjectId(it) } ?: ObjectId(),
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