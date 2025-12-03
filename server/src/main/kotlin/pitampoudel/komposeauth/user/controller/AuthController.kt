package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.json.Json
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.config.login
import pitampoudel.komposeauth.core.config.logout
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.OAuth2Response
import pitampoudel.komposeauth.data.ResponseType
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.dto.mapToProfileResponseDto
import pitampoudel.komposeauth.user.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

@RestController
class AuthController(
    val json: Json,
    val userService: UserService,
    val oneTimeTokenService: OneTimeTokenService,
    val kycService: KycService,
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
        val user = userService.resolveUserFromCredential(
            request = request,
            loadPublicKeyCredentialRequestOptions = {
                val options = requestOptionsRepository.load(httpServletRequest)
                requestOptionsRepository.save(httpServletRequest, httpServletResponse, null)
                options
            }
        )
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
                httpServletRequest.login(accessToken, httpServletResponse)
            }
        }
        return ResponseEntity.ok(user.mapToProfileResponseDto(kycService.isVerified(user.id)))
    }

    @PostMapping("/${ApiEndpoints.LOGOUT}")
    @Operation(
        summary = "Logout the current user",
        description = "Logout the current user by clearing cookies and sessions."
    )
    fun logout(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ResponseEntity<MessageResponse> {
        httpServletRequest.logout(httpServletResponse)
        return ResponseEntity.ok(MessageResponse("Logout successful"))
    }
}
