package com.vardansoft.authx.user.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.vardansoft.authx.AppProperties
import com.vardansoft.authx.core.service.sms.PhoneNumberVerificationService
import com.vardansoft.authx.core.utils.validateGoogleIdToken
import com.vardansoft.authx.data.CreateUserRequest
import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.data.VerifyPhoneOtpRequest
import com.vardansoft.authx.user.dto.UpdateUserRequest
import com.vardansoft.authx.user.dto.UserResponse
import com.vardansoft.authx.user.dto.mapToEntity
import com.vardansoft.authx.user.dto.mapToResponseDto
import com.vardansoft.authx.user.dto.update
import com.vardansoft.authx.user.entity.User
import com.vardansoft.authx.user.repository.UserRepository
import com.vardansoft.core.data.parsePhoneNumber
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    val passwordEncoder: PasswordEncoder,
    private val phoneNumberVerificationService: PhoneNumberVerificationService,
    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val googleClientId: String,
    val appProperties: AppProperties
) {
    fun findUser(id: String): User? {
        return userRepository.findById(ObjectId(id)).orElse(null)
    }

    fun findOrCreateUserByAuthCode(code: String, codeVerifier: String, redirectUri: String): User {
        val client = HttpClient.newHttpClient()
        val form = String.format(
            "client_id=%s&grant_type=authorization_code&code=%s&redirect_uri=%s&code_verifier=%s&client_secret=%s",
            URLEncoder.encode(appProperties.googleAuthDesktopClientId, StandardCharsets.UTF_8),
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8),
            URLEncoder.encode(appProperties.googleAuthDesktopClientSecret, StandardCharsets.UTF_8)
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Failed to exchange auth code: HTTP ${response.statusCode()} - ${response.body()}")
        }
        val mapper = ObjectMapper()
        val node = mapper.readTree(response.body())
        val idToken = node.get("id_token")?.asText()
            ?: throw IllegalStateException("No id_token in token response")
        return findOrCreateUserByGoogleIdToken(idToken)
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

    fun findOrCreateUserByGoogleIdToken(idToken: String): User {
        val payload = validateGoogleIdToken(
            clientIds = listOf(googleClientId, appProperties.googleAuthDesktopClientId),
            idToken = idToken
        )
        val user = findOrCreateUser(
            CreateUserRequest(
                email = payload["email"] as String,
                firstName = payload["given_name"] as String,
                lastName = payload["family_name"] as String?,
                picture = payload["picture"] as? String
            )
        )

        if (payload["email_verified"] as? Boolean == true) {
            emailVerified(user.id)
        }
        return user
    }

}
