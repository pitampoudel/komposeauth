package com.vardansoft.authx.user.service

import com.vardansoft.authx.core.utils.parsePhoneNumber
import com.vardansoft.authx.user.dto.CreateUserRequest
import com.vardansoft.authx.user.dto.UpdatePhoneNumberRequest
import com.vardansoft.authx.user.dto.UpdateUserRequest
import com.vardansoft.authx.user.dto.UserResponse
import com.vardansoft.authx.user.dto.VerifyPhoneOtpRequest
import com.vardansoft.authx.user.dto.mapToEntity
import com.vardansoft.authx.user.dto.mapToResponseDto
import com.vardansoft.authx.user.dto.update
import com.vardansoft.authx.user.entity.User
import com.vardansoft.authx.user.repository.UserRepository
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    val passwordEncoder: PasswordEncoder,
    private val phoneOtpService: PhoneOtpService
) {

    fun findUser(id: String): User? {
        return userRepository.findById(ObjectId(id)).orElse(null)
    }

    fun findUsersBulk(ids: List<String>): List<User> {
        val objectIds = ids.mapNotNull { id ->
            try {
                ObjectId(id)
            } catch (e: Exception) {
                null // Skip invalid IDs
            }
        }
        return userRepository.findByIdIn(objectIds)
    }

    fun findUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun createUser(@Valid req: CreateUserRequest): User {
        return userRepository.insert(req.mapToEntity(passwordEncoder))
    }

    fun updateUser(userId: ObjectId, @Valid req: UpdateUserRequest): UserResponse {
        val existingUser = userRepository.findById(userId).orElse(null) ?: throw IllegalStateException("User not found")
        val result = userRepository.save(existingUser.update(req, passwordEncoder))
        return result.mapToResponseDto()
    }

    fun emailVerified(userId: ObjectId) {
        userRepository.save(
            userRepository.findById(userId).orElseThrow().copy(
                emailVerified = true
            )
        )
    }

    fun findOrCreateUserByEmail(
        email: String,
        firstName: String,
        lastName: String?,
        picture: String?
    ): User {
        return findUserByEmail(email) ?: createUser(
            CreateUserRequest(
                firstName = firstName,
                lastName = lastName,
                email = email,
                picture = picture
            )
        )
    }

    fun initiatePhoneNumberUpdate(user: User, @Valid req: UpdatePhoneNumberRequest): Boolean {
        val parsedPhone = parsePhoneNumber(req.countryCode, req.phoneNumber)
            ?: throw IllegalArgumentException("Invalid phone number format")
        return phoneOtpService.generateAndSendOtp(
            user = user,
            phoneNumber = parsedPhone.fullNumberInInternationalFormat
        )
    }

    fun verifyPhoneNumberUpdate(
        userId: ObjectId,
        @Valid req: VerifyPhoneOtpRequest
    ): UserResponse {
        val user = userRepository.findById(userId).orElse(null) ?: throw IllegalStateException("User not found")
        val phoneOtp = phoneOtpService.verifyOtp(userId, req.otp) ?: run {
            throw IllegalArgumentException("Invalid or expired OTP")
        }

        val updatedUser = user.copy(
            phoneNumber = phoneOtp.phoneNumber,
            phoneNumberVerified = true,
            updatedAt = Instant.now()
        )

        val result = userRepository.save(updatedUser)
        return result.mapToResponseDto()
    }
}
