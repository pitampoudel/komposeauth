package com.vardansoft.authx.user.service

import com.vardansoft.authx.core.service.sms.PhoneNumberVerificationService
import com.vardansoft.authx.core.utils.parsePhoneNumber
import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.data.VerifyPhoneOtpRequest
import com.vardansoft.authx.data.CreateUserRequest
import com.vardansoft.authx.user.dto.UpdateUserRequest
import com.vardansoft.authx.user.dto.UserResponse
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
    private val phoneNumberVerificationService: PhoneNumberVerificationService
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

    fun findUserByEmailOrPhone(value: String): User? {
        return userRepository.findByEmail(value) ?: userRepository.findByPhoneNumber(value)
    }

    fun createUser(@Valid req: CreateUserRequest): User {
        return userRepository.insert(req.mapToEntity(passwordEncoder))
    }

    fun updateUser(userId: ObjectId, @Valid req: UpdateUserRequest): UserResponse {
        val existingUser = userRepository.findById(userId).orElse(null)
            ?: throw IllegalStateException("User not found")
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

    fun findOrCreateUser(req: CreateUserRequest): User {
        val emailOrPhone = req.email ?: req.phoneNumber?.let {
            parsePhoneNumber(
                countryNameCode = null,
                phoneNumber = it
            )?.fullNumberInInternationalFormat
        }
        return emailOrPhone?.let { findUserByEmailOrPhone(emailOrPhone) } ?: createUser(req)
    }

    fun initiatePhoneNumberUpdate(@Valid req: UpdatePhoneNumberRequest): Boolean {
        val parsedPhone = parsePhoneNumber(req.countryCode, req.phoneNumber)
            ?: throw IllegalArgumentException("Invalid phone number format")
        return phoneNumberVerificationService.initiate(
            phoneNumber = parsedPhone.fullNumberInInternationalFormat
        )
    }

    fun verifyPhoneNumberUpdate(
        userId: ObjectId,
        @Valid req: VerifyPhoneOtpRequest
    ): UserResponse {
        val parsedPhoneNumber = parsePhoneNumber(
            countryNameCode = req.countryCode,
            phoneNumber = req.phoneNumber
        )?.fullNumberInInternationalFormat ?: throw IllegalArgumentException(
            "Invalid phone number format"
        )
        val user = userRepository.findById(userId).orElse(null)
            ?: throw IllegalStateException("User not found")
        val verified = phoneNumberVerificationService.verify(
            parsedPhoneNumber,
            req.otp
        )
        if (!verified) throw IllegalArgumentException("Invalid or expired OTP")
        val updatedUser = user.copy(
            phoneNumber = parsedPhoneNumber,
            phoneNumberVerified = true,
            updatedAt = Instant.now()
        )

        val result = userRepository.save(updatedUser)
        return result.mapToResponseDto()
    }
}
