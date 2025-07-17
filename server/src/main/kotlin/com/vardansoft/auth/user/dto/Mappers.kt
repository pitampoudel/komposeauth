package com.vardansoft.auth.user.dto

import com.vardansoft.auth.core.utils.parsePhoneNumber
import com.vardansoft.auth.user.entity.User
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder

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

fun User.update(req: UpdateUserRequest, passwordEncoder: PasswordEncoder): @Valid User {
    return copy(
        firstName = req.firstName ?: firstName,
        lastName = req.lastName ?: lastName,
        email = req.email ?: email,
        passwordHash = req.password?.let {
            passwordEncoder.encode(req.password)
        } ?: passwordHash
    )
}

fun User.mapToResponseDto(): UserResponse {
    return UserResponse(
        id = this.id.toHexString(),
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        emailVerified = this.emailVerified,
        phoneNumber = phoneNumber,
        phoneNumberVerified = phoneNumberVerified,
        photoUrl = this.picture,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}