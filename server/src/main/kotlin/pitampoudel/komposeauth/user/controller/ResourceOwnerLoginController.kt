package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.serialization.json.Json
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.core.data.OAuth2Response
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.core.utils.findServerUrl
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.mapToProfileResponseDto
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@RestController
class ResourceOwnerLoginController(
    val json: Json,
    val userService: UserService,
    val oneTimeTokenService: OneTimeTokenService,
    val kycService: KycService,
    private val requestOptionsRepository: PublicKeyCredentialRequestOptionsRepository,
    val appConfigService: AppConfigService,
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
        val builder = JwtClaimsSet.builder()
            .issuer(findServerUrl(httpServletRequest))
            .subject(user.id.toHexString())
            .issuedAt(now)
            .expiresAt(now + 1.days.toJavaDuration())
            .notBefore(now)
            .claim("authorities", user.roles.map { "ROLE_$it" })
            .claim("scope", scopes)
            .claim("kycVerified", kycService.isVerified(user.id))
            .claim("phoneNumberVerified", user.phoneNumberVerified)

        user.email?.let {
            builder.claim("email", it)
        }
        user.firstName?.let {
            builder.claim("givenName", it)
        }
        user.lastName?.let {
            builder.claim("familyName", it)
        }
        user.picture?.let {
            builder.claim("picture", it)
        }

        val claims = builder.build()

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

            ResponseType.COOKIE -> {
                val cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
                    .httpOnly(true)
                    .secure(httpServletRequest.isSecure)
                    .path("/")
                    .sameSite(if (httpServletRequest.isSecure) "None" else "Lax")
                    .maxAge((1.days - 1.minutes).toJavaDuration())
                    .build()
                httpServletResponse.addHeader("Set-Cookie", cookie.toString())
            }
        }
        return ResponseEntity.ok(user.mapToProfileResponseDto(kycService.isVerified(user.id)))
    }
}
