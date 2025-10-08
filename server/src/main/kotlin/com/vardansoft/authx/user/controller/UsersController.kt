package com.vardansoft.authx.user.controller

import com.vardansoft.authx.core.service.JwtService
import com.vardansoft.authx.data.ApiEndpoints
import com.vardansoft.authx.data.CreateUserRequest
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.OAuth2TokenData
import com.vardansoft.authx.data.TokenRefreshRequest
import com.vardansoft.authx.data.UserInfoResponse
import com.vardansoft.authx.data.UserResponse
import com.vardansoft.authx.kyc.service.KycService
import com.vardansoft.authx.oauth_clients.entity.OAuth2Client.Companion.SCOPE_READ_ANY_USER
import com.vardansoft.authx.user.dto.mapToResponseDto
import com.vardansoft.authx.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant


@Controller
class UsersController(
    val userService: UserService,
    val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    val kycService: KycService
) {
    @PostMapping("/users")
    @Operation(
        summary = "Create user",
        description = "Creates a new user account",
    )
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(userService.createUser(request).mapToResponseDto())

    }


    @PostMapping("/${ApiEndpoints.TOKEN}")
    @Operation(
        summary = "Login with credentials",
        description = "Validate credentials (email/password, Google ID token, or Google OAuth2 authorization code) and returns JWT tokens directly"
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
                redirectUri = request.redirectUri
            )
        } ?: throw UsernameNotFoundException("User not found or invalid credentials")

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

    @PostMapping("/${ApiEndpoints.REFRESH_TOKEN}")
    @Operation(
        summary = "Refresh access token",
        description = "Use refresh token to get a new access token"
    )
    fun refreshToken(@RequestBody @Valid request: TokenRefreshRequest): ResponseEntity<OAuth2TokenData> {
        val userId = jwtService.validateRefreshToken(request.refreshToken)
        val user =
            userService.findUser(userId) ?: throw UsernameNotFoundException("User not found")

        val newAccessToken = jwtService.generateAccessToken(user)

        return ResponseEntity.ok(
            OAuth2TokenData(
                accessToken = newAccessToken,
                refreshToken = request.refreshToken, // Keep the same refresh token
                tokenType = "Bearer",
                expiresIn = 3600,
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

}
