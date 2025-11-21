package pitampoudel.komposeauth.user.service

import jakarta.validation.Valid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.coyote.BadRequestException
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.sms.PhoneNumberVerificationService
import pitampoudel.komposeauth.core.utils.validateGoogleIdToken
import pitampoudel.komposeauth.data.CreateUserRequest
import pitampoudel.komposeauth.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.data.UpdateProfileRequest
import pitampoudel.komposeauth.data.UserResponse
import pitampoudel.komposeauth.data.VerifyPhoneOtpRequest
import pitampoudel.komposeauth.domain.Platform
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.config.service.AppConfigProvider
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.user.dto.mapToEntity
import pitampoudel.komposeauth.user.dto.mapToResponseDto
import pitampoudel.komposeauth.user.dto.update
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
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
    val appProperties: AppConfigProvider,
    val emailService: EmailService,
    val oneTimeTokenService: OneTimeTokenService,
    val kycService: KycService,
    val storageService: StorageService
) {
    fun findUser(id: String): User? {
        return userRepository.findById(ObjectId(id)).orElse(null)
    }

    fun findOrCreateUserByAuthCode(
        code: String,
        codeVerifier: String,
        redirectUri: String,
        platform: Platform
    ): User {
        val client = HttpClient.newHttpClient()
        val form = String.format(
            "client_id=%s&grant_type=authorization_code&code=%s&redirect_uri=%s&code_verifier=%s&client_secret=%s",
            URLEncoder.encode(appProperties.googleClientId(platform), StandardCharsets.UTF_8),
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8),
            URLEncoder.encode(appProperties.googleClientSecret(platform), StandardCharsets.UTF_8)
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
        val jsonElement = Json.parseToJsonElement(response.body())
        val idToken = jsonElement.jsonObject["id_token"]?.jsonPrimitive?.content
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

    fun findUsersFlexible(ids: List<String>?, q: String?, page: Int, size: Int): Page<User> {
        val pageSafe = if (page < 0) 0 else page
        val sizeCapped = when {
            size <= 0 -> 50
            size > 200 -> 200
            else -> size
        }
        val pageable: Pageable = PageRequest.of(pageSafe, sizeCapped)

        if (!ids.isNullOrEmpty()) {
            val all = findUsersBulk(ids)
            val start = (pageSafe * sizeCapped).coerceAtMost(all.size)
            val end = (start + sizeCapped).coerceAtMost(all.size)
            val slice = if (start < end) all.subList(start, end) else emptyList()
            return PageImpl(slice, pageable, all.size.toLong())
        }

        if (!q.isNullOrBlank()) {
            return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(
                q,
                q,
                q,
                q,
                pageable
            )
        }

        return userRepository.findAll(pageable)
    }

    fun findByUserName(value: String): User? {
        return userRepository.findByUserName(value)
    }

    fun createUser(req: CreateUserRequest): User {
        val newUser = req.mapToEntity(passwordEncoder)
        if (newUser.email != null && !newUser.emailVerified) {
            val emailSent = emailService.sendSimpleMail(
                to = newUser.email,
                subject = "Email Verification",
                text = "Please click the link to verify your email address: ${
                    oneTimeTokenService.generateEmailVerificationLink(
                        userId = newUser.id
                    )
                }"
            )
            if (!emailSent) {
                throw BadRequestException("Failed to send verification email.")
            }
        }
        return userRepository.insert(newUser)
    }

    fun updateUser(userId: ObjectId, req: UpdateProfileRequest): UserResponse {
        val existingUser = userRepository.findById(userId).orElse(null)
            ?: throw IllegalStateException("User not found")
        val result = userRepository.save(
            existingUser.update(
                req = req,
                passwordEncoder = passwordEncoder,
                picture = req.picture?.let {
                    val file = it.toKmpFile()
                    val blobName = "users/${existingUser.id.toHexString()}/photo"
                    storageService.upload(
                        blobName = blobName,
                        contentType = file.mimeType,
                        bytes = file.byteArray
                    )
                } ?: existingUser.picture
            )
        )
        return result.mapToResponseDto(kycService.isVerified(result.id))
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
        return emailOrPhone?.let { findByUserName(emailOrPhone) } ?: createUser(req)
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
        return result.mapToResponseDto(kycService.isVerified(result.id))
    }

    fun findOrCreateUserByGoogleIdToken(idToken: String): User {
        val payload = validateGoogleIdToken(
            clientIds = listOfNotNull(
                appProperties.googleAuthClientId,
                appProperties.googleAuthDesktopClientId
            ),
            idToken = idToken
        )
        val user = findOrCreateUser(
            CreateUserRequest(
                email = payload["email"] as String,
                firstName = payload["given_name"] as String,
                lastName = payload["family_name"] as String?,
                photoUrl = payload["picture"] as? String
            )
        )

        if (payload["email_verified"] as? Boolean == true && !user.emailVerified) {
            emailVerified(user.id)
            return findUser(user.id.toHexString()) ?: user
        }
        return user
    }

    fun deactivateUser(userId: ObjectId) {
        val user = userRepository.findById(userId).orElseThrow()
        userRepository.save(user.copy(deactivated = true))
    }

}
