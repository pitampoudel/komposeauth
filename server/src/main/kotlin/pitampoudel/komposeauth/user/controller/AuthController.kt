package pitampoudel.komposeauth.user.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.coyote.BadRequestException
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse
import org.springframework.security.web.webauthn.api.PublicKeyCredential
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import pitampoudel.komposeauth.AppProperties
import pitampoudel.komposeauth.core.service.JwtService
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.OAuth2TokenData
import pitampoudel.komposeauth.data.UserResponse
import pitampoudel.komposeauth.user.dto.mapToResponseDto
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService
import javax.security.auth.login.AccountLockedException
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@Controller
class AuthController(
    val userService: UserService,
    val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper,
    private val webAuthnRelyingPartyOperations: WebAuthnRelyingPartyOperations,
    private val requestOptionsRepository: PublicKeyCredentialRequestOptionsRepository,
    val appProperties: AppProperties
) {
    @PostMapping("/token")
    @Operation(
        summary = "Login with credentials",
        description = "Validate credentials and returns JWT tokens."
    )
    fun token(
        @RequestBody
        request: Credential,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ResponseEntity<OAuth2TokenData> {
        val user = resolveUserFromCredential(request, httpServletRequest, httpServletResponse)
        val accessToken = jwtService.generateAccessToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)
        return ResponseEntity.ok(
            OAuth2TokenData(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = "Bearer",
                expiresIn = 1.days.inWholeSeconds,
            )
        )
    }

    @PostMapping("/${ApiEndpoints.LOGIN}")
    @Operation(
        summary = "Login with credentials",
        description = "Validate credentials and returns User Info."
    )
    fun login(
        @RequestBody
        request: Credential,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ResponseEntity<UserResponse?> {

        val user = resolveUserFromCredential(request, httpServletRequest, httpServletResponse)
        val accessToken = jwtService.generateAccessToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)

        val accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
            .domain(appProperties.domain)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .maxAge((1.days - 1.minutes).toJavaDuration())
            .build()

        val refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
            .domain(appProperties.domain)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .maxAge((7.days - 1.minutes).toJavaDuration())
            .build()

        httpServletResponse.addHeader("Set-Cookie", accessCookie.toString())
        httpServletResponse.addHeader("Set-Cookie", refreshCookie.toString())

        return ResponseEntity.ok(user.mapToResponseDto())
    }

    fun resolveUserFromCredential(
        request: Credential,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): User {
        val user = when (request) {
            is Credential.UsernamePassword -> userService.findByUserName(request.username)
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
                val token = request.refreshToken ?: httpServletRequest.cookies?.find {
                    it.name == "REFRESH_TOKEN"
                }?.value ?: throw BadRequestException(
                    "refresh token not found"
                )
                val userId = jwtService.validateRefreshToken(token)
                userService.findUser(userId) ?: throw UsernameNotFoundException("User not found")
            }

            is Credential.AppleId -> throw UnsupportedOperationException("AppleId authentication is not supported yet.")
            is Credential.PublicKey -> {
                val json = objectMapper.readValue(
                    request.authenticationResponseJson,
                    object :
                        TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {}
                )
                val requestOptions = requestOptionsRepository.load(httpServletRequest)
                requestOptionsRepository.save(httpServletRequest, httpServletResponse, null)

                val publicKeyUser = webAuthnRelyingPartyOperations.authenticate(
                    RelyingPartyAuthenticationRequest(
                        requestOptions,
                        json
                    )
                )
                userService.findByUserName(publicKeyUser.name as String)
            }
        } ?: throw UsernameNotFoundException("User not found or invalid credentials")

        if (user.deactivated) {
            throw AccountLockedException("User account is deactivated")
        }
        return user

    }
}
