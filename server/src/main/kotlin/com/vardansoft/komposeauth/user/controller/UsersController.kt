package com.vardansoft.komposeauth.user.controller

import com.vardansoft.komposeauth.core.config.UserContextService
import com.vardansoft.komposeauth.core.service.JwtService
import com.vardansoft.komposeauth.data.ApiEndpoints
import com.vardansoft.komposeauth.data.CreateUserRequest
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.komposeauth.data.KycResponse
import com.vardansoft.komposeauth.data.OAuth2TokenData
import com.vardansoft.komposeauth.data.UpdateProfileRequest
import com.vardansoft.komposeauth.data.UserInfoResponse
import com.vardansoft.komposeauth.data.UserResponse
import com.vardansoft.komposeauth.kyc.service.KycService
import com.vardansoft.komposeauth.oauth_clients.entity.OAuth2Client.Companion.SCOPE_READ_ANY_USER
import com.vardansoft.komposeauth.user.dto.mapToResponseDto
import com.vardansoft.komposeauth.user.service.UserService
import com.vardansoft.core.data.MessageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import javax.security.auth.login.AccountLockedException
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant


@Controller
class UsersController(
    val userService: UserService,
    val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    val kycService: KycService,
    private val userContextService: UserContextService
) {
    @PostMapping("/users")
    @Operation(
        summary = "Create user",
        description = "Creates a new user account",
    )
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(userService.createUser(request).mapToResponseDto())

    }
    @PatchMapping("/users")
    @Operation(
        summary = "Create a user or return existing",
        description = "Creates a new user account or returns existing user",
    )
    fun findOrCreateUser(@RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(userService.findOrCreateUser(request).mapToResponseDto())
    }


    @PostMapping("/${ApiEndpoints.TOKEN}")
    @Operation(
        summary = "Login with credentials",
        description = "Validate credentials and returns JWT tokens directly."
    )
    fun token(@RequestBody request: Credential): ResponseEntity<OAuth2TokenData> {
        val user = when (request) {
            is Credential.UsernamePassword -> userService.findUserByEmailOrPhone(request.username)
                ?.takeIf {
                    passwordEncoder.matches(request.password, it.passwordHash)
                }

            is Credential.GoogleId -> userService.findOrCreateUserByGoogleIdToken(request.idToken)
            is Credential.AuthCode -> userService.findOrCreateUserByAuthCode(
                code = request.code,
                codeVerifier = request.codeVerifier,
                redirectUri = request.redirectUri,
                platform = request.platform
            )

            is Credential.RefreshToken -> {
                val userId = jwtService.validateRefreshToken(request.refreshToken)
                userService.findUser(userId) ?: throw UsernameNotFoundException("User not found")
            }

            is Credential.AppleId -> TODO()
        } ?: throw UsernameNotFoundException("User not found or invalid credentials")

        if (user.deactivated) {
            throw AccountLockedException("User account is deactivated")
        }

        val accessToken = jwtService.generateAccessToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)

        return ResponseEntity.ok(
            OAuth2TokenData(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = "Bearer",
                expiresIn = 1.hours.inWholeSeconds,
            )
        )
    }

    @GetMapping("/users/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Fetch a single user by their ID"
    )
    @Parameter(name = "id", description = "User ID", required = true)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_$SCOPE_READ_ANY_USER')")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserResponse> {
        val user = userService.findUser(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user.mapToResponseDto())
    }

    @GetMapping("/users/batch")
    @Operation(
        summary = "Get multiple users",
        description = "Fetch multiple users by a comma-separated list of IDs"
    )
    @Parameter(name = "ids", description = "Comma-separated list of user IDs", required = true)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_$SCOPE_READ_ANY_USER')")
    fun getUsersBatch(@RequestParam ids: String): ResponseEntity<List<UserResponse>> {
        val userIds = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (userIds.isEmpty()) {
            return ResponseEntity.badRequest().build()
        }

        val users = userService.findUsersBulk(userIds)
        val userResponses = users.map { it.mapToResponseDto() }
        return ResponseEntity.ok(userResponses)
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get user information",
        description = "Returns user information for the authenticated user following OAuth2/OIDC standard"
    )
    @OptIn(ExperimentalTime::class)
    fun getUserInfo(authentication: Authentication): ResponseEntity<UserInfoResponse> {
        val userId = authentication.name
        val user = userService.findUser(userId) ?: return ResponseEntity.notFound().build()

        val userInfo = UserInfoResponse(
            id = user.id.toHexString(),
            email = user.email ?: "",
            givenName = user.firstName,
            familyName = user.lastName,
            phoneNumber = user.phoneNumber,
            emailVerified = user.emailVerified,
            phoneNumberVerified = user.phoneNumberVerified,
            kycVerified = (kycService.find(user.id)?.status == KycResponse.Status.APPROVED),
            picture = user.picture,
            createdAt = user.createdAt.toKotlinInstant(),
            updatedAt = user.updatedAt.toKotlinInstant(),
            socialLinks = user.socialLinks
        )

        return ResponseEntity.ok(userInfo)
    }

    @PostMapping("/${ApiEndpoints.DEACTIVATE}")
    @Operation(
        summary = "Deactivate account",
        description = "Deactivates the currently authenticated user's account."
    )
    fun deactivate(): ResponseEntity<MessageResponse> {
        val user = userContextService.getCurrentUser()
        userService.deactivateUser(user.id)
        return ResponseEntity.ok(MessageResponse("User account deactivated successfully"))
    }

    @PostMapping("/${ApiEndpoints.UPDATE}")
    @Operation(
        summary = "Update current user information"
    )
    fun update(@RequestBody request: UpdateProfileRequest): ResponseEntity<UserResponse> {
        val user = userContextService.getCurrentUser()
        return ResponseEntity.ok(userService.updateUser(user.id, request))
    }


}