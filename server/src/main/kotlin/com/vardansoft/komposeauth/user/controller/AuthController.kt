package com.vardansoft.komposeauth.user.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.vardansoft.komposeauth.core.service.JwtService
import com.vardansoft.komposeauth.data.ApiEndpoints
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.komposeauth.data.OAuth2TokenData
import com.vardansoft.komposeauth.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
import javax.security.auth.login.AccountLockedException
import kotlin.time.Duration.Companion.hours

@Controller
class AuthController(
    val userService: UserService,
    val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper,
    private val webAuthnRelyingPartyOperations: WebAuthnRelyingPartyOperations,
    private val requestOptionsRepository: PublicKeyCredentialRequestOptionsRepository
) {
    @PostMapping("/${ApiEndpoints.TOKEN}")
    @Operation(
        summary = "Login with credentials",
        description = "Validate credentials and returns JWT tokens directly."
    )
    fun token(
        @RequestBody
        request: Credential,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ResponseEntity<OAuth2TokenData> {
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
                val userId = jwtService.validateRefreshToken(request.refreshToken)
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
}
