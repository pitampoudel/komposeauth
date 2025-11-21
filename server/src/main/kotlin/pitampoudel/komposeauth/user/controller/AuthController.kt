package pitampoudel.komposeauth.user.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.json.Json
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse
import org.springframework.security.web.webauthn.api.PublicKeyCredential
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.core.service.JwtService
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.data.OAuth2Response
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.config.service.AppConfigProvider
import pitampoudel.komposeauth.user.dto.mapToProfileResponseDto
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService
import javax.security.auth.login.AccountLockedException
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@RestController
class AuthController(
    val json: Json,
    val userService: UserService,
    val jwtService: JwtService,
    val oneTimeTokenService: OneTimeTokenService,
    val kycService: KycService,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper,
    private val webAuthnRelyingPartyOperations: WebAuthnRelyingPartyOperations,
    private val requestOptionsRepository: PublicKeyCredentialRequestOptionsRepository,
    val appConfigProvider: AppConfigProvider
) {

    @PostMapping("/${ApiEndpoints.LOGIN}")
    @Operation(
        summary = "Login with credentials",
        description = "Validate credentials and returns user profile or tokens."
    )
    fun login(
        @RequestBody
        request: Credential,
        @RequestParam(required = false, defaultValue = false.toString())
        wantToken: Boolean,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ResponseEntity<*> {

        val user = resolveUserFromCredential(request, httpServletRequest, httpServletResponse)
        val accessToken = jwtService.generateAccessToken(user, validity = 1.days)
        val refreshToken = oneTimeTokenService.generateRefreshToken(user.id)

        if (wantToken) {
            return ResponseEntity.ok(
                json.encodeToString(
                    OAuth2Response(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        tokenType = "Bearer",
                        expiresIn = 1.days.inWholeSeconds,
                    )
                )
            )
        }
        val accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
            .domain(appConfigProvider.domain())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .maxAge((1.days - 1.minutes).toJavaDuration())
            .build()

        httpServletResponse.addHeader("Set-Cookie", accessCookie.toString())

        return ResponseEntity.ok(user.mapToProfileResponseDto(kycService.find(user.id)?.status == KycResponse.Status.APPROVED))
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
                val token = oneTimeTokenService.consume(
                    request.refreshToken,
                    purpose = OneTimeToken.Purpose.REFRESH_TOKEN
                )
                userService.findUser(token.userId.toHexString()) ?: throw UsernameNotFoundException(
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
