package pitampoudel.komposeauth.user.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.InternetDomainName
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.json.Json
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse
import org.springframework.security.web.webauthn.api.PublicKeyCredential
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.data.ResponseType
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.OAuth2Response
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.dto.mapToProfileResponseDto
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService
import java.net.URL
import java.time.Instant
import javax.security.auth.login.AccountLockedException
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@RestController
class AuthController(
    val json: Json,
    val userService: UserService,
    val oneTimeTokenService: OneTimeTokenService,
    val kycService: KycService,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper,
    private val webAuthnRelyingPartyOperations: WebAuthnRelyingPartyOperations,
    private val requestOptionsRepository: PublicKeyCredentialRequestOptionsRepository,
    val appConfigProvider: AppConfigProvider,
    private val jwtEncoder: JwtEncoder
) {

    @PostMapping("/${ApiEndpoints.LOGIN}")
    @Operation(
        summary = "Login with credentials",
        description = "Validate credentials and returns user profile or tokens."
    )
    fun login(
        @RequestBody
        request: Credential,
        @RequestParam(required = false, defaultValue = "COOKIE")
        responseType: ResponseType,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        securityContextRepository: HttpSessionSecurityContextRepository
    ): ResponseEntity<*> {
        val user = resolveUserFromCredential(request, httpServletRequest, httpServletResponse)
        val now = Instant.now()
        val scopes = listOf("openid", "profile", "email")
        val claims = JwtClaimsSet.builder()
            .issuer(appConfigProvider.selfBaseUrl)
            .subject(user.id.toHexString())
            .audience(listOf(appConfigProvider.selfBaseUrl))
            .issuedAt(now)
            .expiresAt(now + 1.days.toJavaDuration())
            .notBefore(now)
            .claim("email", user.email)
            .claim("givenName", user.firstName)
            .claim("familyName", user.lastName)
            .claim("picture", user.picture)
            .claim("authorities", user.roles.map { "ROLE_$it" })
            .claim("scope", scopes.joinToString(" "))
            .claim("scp", scopes)
            .claim("kycVerified", kycService.isVerified(user.id))
            .claim("phoneNumberVerified", user.phoneNumberVerified)
            .build()

        val accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
        val refreshToken = oneTimeTokenService.generateRefreshToken(user.id)

        when (responseType) {
            ResponseType.TOKEN -> {
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

            ResponseType.SESSION -> {
                val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(
                        user.email,
                        null,
                        authorities
                    )
                securityContextRepository.saveContext(
                    SecurityContextHolder.getContext(), httpServletRequest, httpServletResponse
                )
            }

            ResponseType.COOKIE -> {
                val isSecure = httpServletRequest.isSecure
                val cookieDomain = resolveCookieDomain(httpServletRequest)
                val cookieBuilder = ResponseCookie.from("ACCESS_TOKEN", accessToken)
                    .httpOnly(true)
                    .secure(isSecure)
                    .path("/")
                    .sameSite("None")
                    .maxAge((1.days - 1.minutes).toJavaDuration())
                val cookie = if (cookieDomain != null) {
                    cookieBuilder.domain(cookieDomain).build()
                } else {
                    cookieBuilder.build()
                }
                httpServletResponse.addHeader("Set-Cookie", cookie.toString())
            }
        }
        return ResponseEntity.ok(user.mapToProfileResponseDto(kycService.isVerified(user.id)))
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
                val publicKeyUser = requestOptions?.let {
                    webAuthnRelyingPartyOperations.authenticate(
                        RelyingPartyAuthenticationRequest(
                            requestOptions,
                            json
                        )
                    )
                }
                publicKeyUser?.let {
                    userService.findByUserName(publicKeyUser.name)
                }
            }
        } ?: throw UsernameNotFoundException("User not found or invalid credentials")

        if (user.deactivated) {
            throw AccountLockedException("User account is deactivated")
        }
        return user

    }

    private fun resolveCookieDomain(request: HttpServletRequest): String? {
        val origin = request.getHeader("Origin")?.takeIf { it.isNotBlank() }
            ?: request.getHeader("Referer")?.takeIf { it.isNotBlank() }
            ?: return null
        val url = runCatching { URL(origin) }.getOrNull() ?: return null
        val host = url.host ?: return null

        // Localhost and IPs must NOT have a Domain attribute.
        if (host.equals("localhost", ignoreCase = true)) return null
        if (host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))) return null

        return try {
            val domain = InternetDomainName.from(host)
            if (domain.isUnderPublicSuffix) {
                domain.topPrivateDomain().toString()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
