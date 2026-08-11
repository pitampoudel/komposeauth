package pitampoudel.komposeauth.user.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.security.access.AccessDeniedException
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
import pitampoudel.core.domain.isValidEmail
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.authorization.OAuth2AuthorizationDocumentRepository
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.core.service.email.EmailVerificationService
import pitampoudel.komposeauth.core.utils.googleProfileFrom
import pitampoudel.komposeauth.core.utils.validateGoogleIdToken
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.repository.OneTimeTokenRepository
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.otp.service.PhoneNumberVerificationService
import pitampoudel.komposeauth.organization.repository.OrganizationRepository
import pitampoudel.komposeauth.user.data.CreateUserRequest
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.data.ProfileResponse
import pitampoudel.komposeauth.user.data.RoleResponse
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.data.UserResponse
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.webauthn.repository.PublicKeyCredentialRepository
import pitampoudel.komposeauth.webauthn.repository.PublicKeyUserRepository
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
    private val kycVerificationRepository: KycVerificationRepository,
    private val publicKeyUserRepository: PublicKeyUserRepository,
    private val publicKeyCredentialRepository: PublicKeyCredentialRepository,
    private val organizationRepository: OrganizationRepository,
    private val oneTimeTokenRepository: OneTimeTokenRepository,
    val storageService: StorageService,
    private val objectMapper: ObjectMapper,
    private val webAuthnRelyingPartyOperations: WebAuthnRelyingPartyOperations,
    private val roleChangeEmailNotifier: RoleChangeEmailNotifier,
    private val emailVerificationService: EmailVerificationService,
    private val appleTokenValidator: AppleTokenValidator,
    private val oauth2AuthorizationDocumentRepository: OAuth2AuthorizationDocumentRepository
) {
    fun findUser(id: String): User? {
        return userRepository.findById(ObjectId(id)).orElse(null)
    }

    fun findOrCreateUserByGoogleAuthCode(
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

    /** Every grantable role, with how many users currently hold it. */
    fun listRoles(): List<RoleResponse> {
        return appConfigService.availableRoles().map { role ->
            RoleResponse(
                role = role,
                userCount = userRepository.countByRolesContaining(role),
                builtIn = role in Roles.BUILT_IN
            )
        }
    }

    fun grantRole(actor: User, userId: String, role: String): User {
        val normalized = requireManageableRole(actor, role)
        val user = requireUser(userId)
        if (user.roles.contains(normalized)) return user
        val saved = userRepository.save(user.copy(roles = user.roles + normalized))

        roleChangeEmailNotifier.notify(
            target = saved,
            action = RoleChangeEmailNotifier.Action.GRANTED,
            actor = actor.fullName,
            role = normalized
        )

        return saved
    }

    fun revokeRole(actor: User, userId: String, role: String): User {
        val normalized = requireManageableRole(actor, role)
        val user = requireUser(userId)
        if (!user.roles.contains(normalized)) return user
        if (normalized in Roles.PROTECTED &&
            userRepository.countByRolesContaining(normalized) <= 1
        ) {
            throw BadRequestException("Cannot remove the last $normalized")
        }
        val saved = userRepository.save(
            user.copy(roles = user.roles.filterNot { it == normalized })
        )

        roleChangeEmailNotifier.notify(
            target = saved,
            action = RoleChangeEmailNotifier.Action.REVOKED,
            actor = actor.fullName,
            role = normalized
        )

        return saved
    }

    private fun requireUser(userId: String): User {
        val id = try {
            ObjectId(userId)
        } catch (_: Exception) {
            throw UsernameNotFoundException("Invalid user id: $userId")
        }
        return userRepository.findById(id).orElseThrow {
            UsernameNotFoundException("User not found: $userId")
        }
    }

    private fun requireManageableRole(actor: User, role: String): String {
        val normalized = Roles.normalize(role)
        if (normalized !in appConfigService.availableRoles()) {
            throw BadRequestException(
                "Unknown role: $normalized. Add it to the role catalog in app config first."
            )
        }
        // SUPER_ADMIN gates access to app configuration and its secrets, so an ADMIN must not be
        // able to hand it to themselves.
        if (normalized == Roles.SUPER_ADMIN && !actor.roles.contains(Roles.SUPER_ADMIN)) {
            throw AccessDeniedException("Only a ${Roles.SUPER_ADMIN} can manage the ${Roles.SUPER_ADMIN} role")
        }
        return normalized
    }

    fun findUsersFlexible(
        ids: List<String>?,
        q: String?,
        role: String? = null,
        page: Int,
        size: Int
    ): Page<User> {
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

        val tokens = q?.trim()
            ?.split("\\s+".toRegex())
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val roleFilter = role?.takeIf { it.isNotBlank() }?.let { Roles.normalize(it) }

        if (tokens.isNotEmpty() || roleFilter != null) {
            return userRepository.search(tokens, roleFilter, pageable)
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
                    "recipientName" to newUser.firstNameOrUser(),
                    "message" to "Please click the button below to verify your email address and continue using our service.",
                    "actionUrl" to if (!newUser.emailVerified) oneTimeTokenService.generateEmailVerificationLink(
                        userId = newUser.id,
                        baseUrl = baseUrl,
                        email = newUser.email
                    ) else null,
                    "actionText" to "Verify Email"
                )
            )
        }
        return newUser
    }

    /**
     * @param requireReauthentication when true, changing the password or email of an account that
     * already has a password requires the caller to supply that password. Reset-password flows pass
     * false: possession of the emailed one-time token is the proof there.
     */
    fun updateUser(
        userId: ObjectId,
        req: UpdateProfileRequest,
        requireReauthentication: Boolean = true
    ): ProfileResponse {
        val existingUser = userRepository.findById(userId).orElse(null)
            ?: throw IllegalStateException("User not found")

        if (requireReauthentication) {
            val changesCredentials = req.password != null ||
                    (req.email != null && req.email != existingUser.email)
            val currentHash = existingUser.passwordHash
            // A passwordless account (social or OTP sign-in) has nothing to check against; for one
            // with a password, a hijacked session must not be enough to seize the account.
            if (changesCredentials && currentHash != null) {
                val supplied = req.currentPassword
                if (supplied.isNullOrEmpty() || !passwordEncoder.matches(supplied, currentHash)) {
                    throw AccessDeniedException("Current password is incorrect")
                }
            }
        }

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

    fun markEmailVerified(user: User, email: String): User {
        if (user.emailVerified && user.email == email) return user
        val updatedUser = user.copy(
            email = email,
            emailVerified = true,
            updatedAt = Instant.now()
        )
        return userRepository.save(updatedUser)
    }

    fun findOrCreateUser(baseUrl: String?, req: CreateUserRequest): User {
        return req.findPrimaryUsername()?.let {
            findByUserName(it)
        } ?: req.findAlternateUsername()?.let {
            findByUserName(it)
        } ?: createUser(baseUrl, req)
    }

    fun verifyPhoneNumber(
        userId: ObjectId,
        phoneNumber: String,
        otp: String
    ): UserResponse {
        val user = userRepository.findById(userId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val verified = phoneNumberVerificationService.verify(
            phoneNumber,
            otp
        )
        if (!verified) throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid or expired OTP"
        )
        val updatedUser = user.copy(
            phoneNumber = phoneNumber,
            phoneNumberVerified = true,
            updatedAt = Instant.now()
        )

        val result = userRepository.save(updatedUser)
        return result.mapToResponseDto(kycService.isVerified(result.id))
    }

    fun verifyEmail(
        userId: ObjectId,
        email: String,
        otp: String
    ): UserResponse {
        val user = userRepository.findById(userId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val verified = emailVerificationService.verify(email, otp)
        if (!verified) throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid or expired OTP"
        )
        val updatedUser = user.copy(
            email = email,
            emailVerified = true,
            updatedAt = Instant.now()
        )
        val result = userRepository.save(updatedUser)
        return result.mapToResponseDto(kycService.isVerified(result.id))
    }

    /**
     * For a raw ID token this server has to check itself — what a native client posts to the login
     * API. The browser sign-in flow uses [findOrCreateVerifiedGoogleUser] instead; see the note
     * there.
     */
    fun findOrCreateUserByGoogleIdToken(idToken: String): User {
        val payload = validateGoogleIdToken(
            clientIds = listOfNotNull(
                appConfigService.getConfig().googleAuthClientId,
                appConfigService.getConfig().googleAuthDesktopClientId
            ),
            idToken = idToken
        )
        return findOrCreateVerifiedGoogleUser(
            profile = googleProfileFrom(payload),
            emailVerified = payload.emailVerified == true
        )
    }

    /**
     * Provisioning from a Google profile whose token has already been verified.
     *
     * The browser sign-in flow comes in here. Spring Security's OIDC login checks the ID token's
     * signature, issuer, audience, nonce and expiry — against a key set it caches — before the
     * success handler runs, so verifying it a second time proved nothing and made every sign-in
     * depend on a live call to Google's certificate endpoint. When that call was slow or failed,
     * the visitor was told we couldn't sign them in, for a token that was perfectly good.
     */
    fun findOrCreateVerifiedGoogleUser(profile: CreateUserRequest, emailVerified: Boolean): User {
        val user = findOrCreateUser(baseUrl = null, req = profile)
        val email = profile.email

        if (emailVerified && email != null && !user.emailVerified) {
            markEmailVerified(user, email)
            return findUser(user.id.toHexString()) ?: user
        }
        return user
    }

    private fun findOrCreateUserByAppleIdToken(idToken: String): User {
        val claims = appleTokenValidator.validate(
            idToken = idToken,
            clientId = appConfigService.getConfig().appleAuthClientId
                ?: throw IllegalStateException("Apple client id not configured")
        )
        val email = claims.getStringClaim("email")

        val user = findOrCreateUser(
            baseUrl = null,
            CreateUserRequest(
                email = email,
                firstName = null,
                lastName = null,
                photoUrl = null
            )
        )

        // `== true` rather than a bare call: the claim is optional, and unboxing a null Boolean here
        // would throw the same way the Google path did.
        if (claims.getBooleanClaim("email_verified") == true && !user.emailVerified) {
            markEmailVerified(user, email)
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
            is Credential.AuthCode -> findOrCreateUserByGoogleAuthCode(
                code = request.code,
                redirectUri = request.redirectUri,
                platform = request.platform
            )

            is Credential.RefreshToken -> {
                val token = oneTimeTokenService.consume(
                    request.refreshToken,
                    purpose = OneTimeToken.Purpose.REFRESH_TOKEN
                )
                findUser(token.userId.toHexString()) ?: throw AccessDeniedException(
                    "Invalid credentials"
                )
            }

            is Credential.AppleId -> findOrCreateUserByAppleIdToken(request.idToken)
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

            is Credential.OTP -> {
                resolveOtpLogin(request.username, request.otp)
            }
        } ?: throw AccessDeniedException("Invalid credentials")

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

    fun deleteUser(userId: ObjectId) {
        val user = userRepository.findById(userId).orElseThrow()

        kycVerificationRepository.findByUserId(user.id)?.let { kyc ->
            listOfNotNull(
                kyc.documentFrontUrl,
                kyc.documentBackUrl,
                kyc.selfieUrl
            ).forEach {
                storageService.delete(it)
            }
            kycVerificationRepository.deleteById(user.id)
        }

        publicKeyCredentialRepository.deleteAllByUserId(user.id)

        publicKeyUserRepository.findByUserId(user.id)?.let { publicKeyUser ->
            publicKeyCredentialRepository.deleteAllByPublicKeyUserId(publicKeyUser.userHandle)
            publicKeyUserRepository.deleteById(publicKeyUser.id)
        }

        organizationRepository.findAllByUserIdsContains(user.id).forEach { organization ->
            organizationRepository.save(
                organization.copy(
                    userIds = organization.userIds.filterNot { id -> id == user.id }
                )
            )

            // todo if org has no users and not being used delete it
        }

        user.picture?.let { storageService.delete(it) }

        oneTimeTokenRepository.deleteAllByUserId(user.id)
        oauth2AuthorizationDocumentRepository.deleteAllByPrincipalName(user.id.toHexString())

        userRepository.deleteById(user.id)
    }

    private fun resolveOtpLogin(username: String, otp: String): User {
        val normalizedEmail = username.lowercase().takeIf { it.isValidEmail() }
        val normalizedPhone = parsePhoneNumber(null, username)?.fullNumberInE164Format

        if (normalizedPhone != null && phoneNumberVerificationService.verify(
                phoneNumber = normalizedPhone,
                code = otp
            )
        ) {
            return findOrCreateVerifiedOtpUser(email = null, phoneNumber = normalizedPhone)
        }
        if (normalizedEmail != null && emailVerificationService.verify(normalizedEmail, otp)) {
            return findOrCreateVerifiedOtpUser(email = normalizedEmail, phoneNumber = null)
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP")
    }

    private fun findOrCreateVerifiedOtpUser(email: String?, phoneNumber: String?): User {
        val username = email ?: phoneNumber
        ?: throw IllegalArgumentException("Either email or phone number is required")
        val existingUser = findByUserName(username)
        if (existingUser != null) {
            val updatedUser = existingUser.copy(
                email = existingUser.email ?: email,
                emailVerified = existingUser.emailVerified || email != null,
                phoneNumber = existingUser.phoneNumber ?: phoneNumber,
                phoneNumberVerified = existingUser.phoneNumberVerified || phoneNumber != null,
                updatedAt = Instant.now()
            )
            return if (updatedUser == existingUser) existingUser else userRepository.save(
                updatedUser
            )
        }

        val newUser = User(
            id = ObjectId(),
            firstName = null,
            lastName = null,
            email = email,
            emailVerified = email != null,
            phoneNumber = phoneNumber,
            phoneNumberVerified = phoneNumber != null,
        )
        return userRepository.insert(newUser)
    }
}
