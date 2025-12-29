package pitampoudel.komposeauth.user.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse
import org.springframework.security.web.webauthn.api.PublicKeyCredential
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.user.data.CreateUserRequest
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.data.ProfileResponse
import pitampoudel.komposeauth.user.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.data.UserResponse
import pitampoudel.komposeauth.user.data.VerifyPhoneOtpRequest
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.core.service.sms.PhoneNumberVerificationService
import pitampoudel.komposeauth.core.utils.validateGoogleIdToken
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.dto.mapToEntity
import pitampoudel.komposeauth.user.dto.mapToProfileResponseDto
import pitampoudel.komposeauth.user.dto.mapToResponseDto
import pitampoudel.komposeauth.user.dto.update
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.security.auth.login.AccountLockedException

@Service
class UserService(
    private val userRepository: UserRepository,
    val passwordEncoder: PasswordEncoder,
    private val phoneNumberVerificationService: PhoneNumberVerificationService,
    val appConfigService: AppConfigService,
    val emailService: EmailService,
    val oneTimeTokenService: OneTimeTokenService,
    val kycService: KycService,
    val storageService: StorageService,
    private val objectMapper: ObjectMapper,
    private val webAuthnRelyingPartyOperations: WebAuthnRelyingPartyOperations,
    private val roleChangeEmailNotifier: RoleChangeEmailNotifier,
) {
    fun findUser(id: String): User? {
        return userRepository.findById(ObjectId(id)).orElse(null)
    }

    fun findOrCreateUserByAuthCode(
        code: String,
        redirectUri: String,
        platform: Platform
    ): User {
        val client = HttpClient.newHttpClient()
        val form = String.format(
            "client_id=%s&grant_type=authorization_code&code=%s&redirect_uri=%s&client_secret=%s",
            URLEncoder.encode(appConfigService.googleClientId(platform), StandardCharsets.UTF_8),
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(
                appConfigService.googleClientSecret(platform),
                StandardCharsets.UTF_8
            )
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
            } catch (_: Exception) {
                null // Skip invalid IDs
            }
        }
        return userRepository.findByIdIn(objectIds)
    }

    fun listAdmins(page: Int, size: Int): Page<User> {
        val pageable: Pageable = PageRequest.of(page, size)
        return userRepository.findByRolesContaining("ADMIN", pageable)
    }

    fun grantAdmin(actor: String, userId: String): User {
        val id = try {
            ObjectId(userId)
        } catch (_: Exception) {
            throw UsernameNotFoundException("Invalid user id: $userId")
        }
        val user = userRepository.findById(id).orElseThrow {
            UsernameNotFoundException("User not found: $userId")
        }
        if (user.roles.contains("ADMIN")) return user
        val updated = user.copy(roles = user.roles + "ADMIN")
        val saved = userRepository.save(updated)

        roleChangeEmailNotifier.notify(
            target = saved,
            action = RoleChangeEmailNotifier.Action.GRANTED,
            actor = actor
        )

        return saved
    }

    fun revokeAdmin(actor: String, userId: String): User {
        val id = try {
            ObjectId(userId)
        } catch (_: Exception) {
            throw UsernameNotFoundException("Invalid user id: $userId")
        }
        val user = userRepository.findById(id).orElseThrow {
            UsernameNotFoundException("User not found: $userId")
        }
        if (!user.roles.contains("ADMIN")) return user
        val totalAdmins = userRepository.countByRolesContaining("ADMIN")
        if (totalAdmins <= 1) {
            throw BadRequestException("Cannot remove the last admin")
        }
        val updated = user.copy(roles = user.roles.filterNot { it == "ADMIN" })
        val saved = userRepository.save(updated)


        roleChangeEmailNotifier.notify(
            target = saved,
            action = RoleChangeEmailNotifier.Action.REVOKED,
            actor = actor
        )

        return saved
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

    fun createUser(baseUrl: String?, req: CreateUserRequest): User {
        var newUser = req.mapToEntity(passwordEncoder)
        newUser = userRepository.insert(newUser)
        if (newUser.email != null && baseUrl != null) {
            emailService.sendHtmlMail(
                baseUrl = baseUrl,
                to = newUser.email,
                subject = "Welcome to ${appConfigService.getConfig().name}!",
                template = "email/generic",
                model = mapOf(
                    "recipientName" to newUser.firstName,
                    "message" to "Please click the button below to verify your email address and continue using our service.",
                    "actionUrl" to if (!newUser.emailVerified) oneTimeTokenService.generateEmailVerificationLink(
                        userId = newUser.id,
                        baseUrl = baseUrl
                    ) else null,
                    "actionText" to "Verify Email"
                )
            )
        }
        return newUser
    }

    fun updateUser(userId: ObjectId, req: UpdateProfileRequest): ProfileResponse {
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
        return result.mapToProfileResponseDto(kycService.isVerified(result.id))
    }

    fun emailVerified(userId: ObjectId) {
        userRepository.save(
            userRepository.findById(userId).orElseThrow().copy(
                emailVerified = true
            )
        )
    }

    fun findOrCreateUser(baseUrl: String?, req: CreateUserRequest): User {
        return req.findPrimaryUsername()?.let {
            findByUserName(it)
        } ?: req.findAlternateUsername()?.let {
            findByUserName(it)
        } ?: createUser(baseUrl, req)
    }

    fun initiatePhoneNumberUpdate(@Valid req: UpdatePhoneNumberRequest): Boolean {
        val parsedPhone = parsePhoneNumber(req.countryCode, req.phoneNumber)
            ?: throw IllegalArgumentException("Invalid phone number format")
        if (findByUserName(parsedPhone.fullNumberInE164Format) != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is already in use")
        }
        return phoneNumberVerificationService.initiate(
            phoneNumber = parsedPhone.fullNumberInE164Format
        )
    }

    fun verifyPhoneNumberUpdate(
        userId: ObjectId,
        @Valid req: VerifyPhoneOtpRequest
    ): UserResponse {
        val parsedPhoneNumber = req.parsedPhoneNumber() ?: throw IllegalArgumentException(
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
                appConfigService.getConfig().googleAuthClientId,
                appConfigService.getConfig().googleAuthDesktopClientId
            ),
            idToken = idToken
        )
        val user = findOrCreateUser(
            baseUrl = null,
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

    fun resolveUserFromCredential(
        request: Credential,
        loadPublicKeyCredentialRequestOptions: () -> PublicKeyCredentialRequestOptions?
    ): User {
        val user = when (request) {
            is Credential.UsernamePassword -> findByUserName(request.username())
                ?.takeIf {
                    passwordEncoder.matches(request.password, it.passwordHash)
                }

            is Credential.GoogleId -> findOrCreateUserByGoogleIdToken(request.idToken)
            is Credential.AuthCode -> findOrCreateUserByAuthCode(
                code = request.code,
                redirectUri = request.redirectUri,
                platform = request.platform
            )

            is Credential.RefreshToken -> {
                val token = oneTimeTokenService.consume(
                    request.refreshToken,
                    purpose = OneTimeToken.Purpose.REFRESH_TOKEN
                )
                findUser(token.userId.toHexString()) ?: throw UsernameNotFoundException(
                    "User not found"
                )
            }

            is Credential.AppleId -> throw UnsupportedOperationException("AppleId authentication is not supported yet.")
            is Credential.PublicKey -> {
                val json = objectMapper.readValue(
                    request.authenticationResponseJson,
                    object :
                        TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {}
                )
                val requestOptions = loadPublicKeyCredentialRequestOptions()
                val publicKeyUser = requestOptions?.let {
                    webAuthnRelyingPartyOperations.authenticate(
                        RelyingPartyAuthenticationRequest(
                            requestOptions,
                            json
                        )
                    )
                }
                publicKeyUser?.let {
                    findByUserName(publicKeyUser.name)
                }
            }
        } ?: throw UsernameNotFoundException("User not found or invalid credentials")

        if (user.deactivated) {
            throw AccountLockedException("User account is deactivated")
        }
        return user

    }

    fun countUsers(): Long {
        return userRepository.count()
    }

    fun deactivateUser(userId: ObjectId) {
        val user = userRepository.findById(userId).orElseThrow()
        userRepository.save(user.copy(deactivated = true))
    }

}
