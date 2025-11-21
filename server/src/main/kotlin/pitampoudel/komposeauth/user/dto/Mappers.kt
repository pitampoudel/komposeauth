package pitampoudel.komposeauth.user.dto

import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.data.CreateUserRequest
import pitampoudel.komposeauth.data.ProfileResponse
import pitampoudel.komposeauth.data.UpdateProfileRequest
import pitampoudel.komposeauth.data.UserResponse
import pitampoudel.komposeauth.user.entity.User
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
        picture = photoUrl,
        passwordHash = password?.let { passwordEncoder.encode(it) }
    )
}

fun User.update(
    req: UpdateProfileRequest,
    passwordEncoder: PasswordEncoder,
    picture: String?
): @Valid User {
    return copy(
        firstName = req.givenName ?: firstName,
        lastName = req.familyName ?: lastName,
        email = req.email ?: email,
        emailVerified = if (req.email == null || req.email == email) emailVerified else false,
        passwordHash = req.password?.let {
            passwordEncoder.encode(req.password)
        } ?: passwordHash,
        picture = picture
    )
}

@OptIn(ExperimentalTime::class)
fun User.mapToResponseDto(kycVerified: Boolean): UserResponse {
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
        updatedAt = this.updatedAt.toKotlinInstant(),
        kycVerified = kycVerified
    )
}

@OptIn(ExperimentalTime::class)
fun User.mapToProfileResponseDto(kycVerified: Boolean): ProfileResponse {
    return ProfileResponse(
        id = id.toHexString(),
        email = email ?: "",
        givenName = firstName,
        familyName = lastName,
        phoneNumber = phoneNumber,
        emailVerified = emailVerified,
        phoneNumberVerified = phoneNumberVerified,
        kycVerified = kycVerified,
        picture = picture,
        createdAt = createdAt.toKotlinInstant(),
        updatedAt = updatedAt.toKotlinInstant(),
        socialLinks = socialLinks,
        roles = roles
    )
}