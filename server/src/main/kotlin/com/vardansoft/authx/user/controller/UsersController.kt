package com.vardansoft.authx.user.controller

import com.vardansoft.authx.core.service.EmailService
import com.vardansoft.authx.core.service.JwtService
import com.vardansoft.authx.core.utils.acceptsHtml
import com.vardansoft.authx.data.CreateUserRequest
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.data.OAuth2TokenData
import com.vardansoft.authx.data.TokenRefreshRequest
import com.vardansoft.authx.data.UserInfoResponse
import com.vardansoft.authx.kyc.service.KycService
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.user.dto.UserResponse
import com.vardansoft.authx.user.dto.mapToResponseDto
import com.vardansoft.authx.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime


@Controller
class UsersController(
    val userService: UserService,
    val emailService: EmailService,
    val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    val kycService: KycService
) {

    @GetMapping("/signup")
    fun create(): String {
        return "signup"
    }

    @GetMapping("/login")
    fun login(): String {
        return "login"
    }

    @PostMapping("/users")
    fun create(
        @ModelAttribute request: CreateUserRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        val createdUser = userService.findOrCreateUser(request)
        val userResponse = createdUser.mapToResponseDto()
        if (createdUser.email != null && !createdUser.emailVerified) emailService.sendSimpleMail(
            to = createdUser.email,
            subject = "Email Verification",
            text = "Please click the link to verify your email address: ${
                jwtService.generateEmailVerificationLink(
                    userId = createdUser.id.toHexString()
                )
            }"
        )
        return if (httpRequest.acceptsHtml()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/login")
                .body("Redirecting to /login")
        } else {
            ResponseEntity.ok().body(userResponse)
        }
    }


    @PostMapping("/token")
    @Operation(
        summary = "Login with credentials",
        description = "Validate credentials and returns JWT tokens directly"
    )
    fun token(@RequestBody request: Credential): ResponseEntity<OAuth2TokenData> {
        val user = when (request) {
            is Credential.EmailPassword -> userService.findUserByEmailOrPhone(request.username)
                ?.takeIf {
                    passwordEncoder.matches(request.password, it.passwordHash)
                }

            is Credential.GoogleId -> userService.findOrCreateUserByGoogleIdToken(request.idToken)
        } ?: throw UsernameNotFoundException("User not found or invalid credentials")

        val accessToken = jwtService.generateAccessToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)

        return ResponseEntity.ok(
            OAuth2TokenData(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = "Bearer",
                expiresIn = 1.hours.inWholeSeconds,
                scope = ""
            )
        )
    }

    @PostMapping("/refresh_token")
    @Operation(
        summary = "Refresh access token",
        description = "Use refresh token to get new access token"
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
                scope = ""
            )
        )
    }


    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user.read.any')")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserResponse> {
        val user = userService.findUser(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user.mapToResponseDto())
    }

    @GetMapping("/users/batch")
    @PreAuthorize("hasAuthority('SCOPE_user.read.any')")
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
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber,
            emailVerified = user.emailVerified,
            phoneNumberVerified = user.phoneNumberVerified,
            kycVerified = (kycService.find(user.id)?.status == KycResponse.Status.APPROVED),
            picture = user.picture,
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(user.createdAt.toEpochMilli()),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(user.updatedAt.toEpochMilli()),
            socialLinks = user.socialLinks
        )

        return ResponseEntity.ok(userInfo)
    }

}
