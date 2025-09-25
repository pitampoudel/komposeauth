package com.vardansoft.authx.core.service

import com.vardansoft.authx.AppProperties
import com.vardansoft.authx.user.entity.User
import org.apache.coyote.BadRequestException
import org.springframework.http.HttpStatusCode
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Service
class JwtService(
    val appProperties: AppProperties,
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: JwtDecoder
) {
    enum class TokenType {
        VERIFY_EMAIL,
        RESET_PASSWORD,
        ACCESS_TOKEN,
        REFRESH_TOKEN
    }

    private fun generateToken(
        audience: String,
        userId: String,
        claims: Map<String, Any?>,
        validity: Duration,
    ): Jwt? {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(validity.inWholeSeconds)

        val jwt = jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwtClaimsSet.builder()
                    .issuer(appProperties.baseUrl)
                    .issuedAt(now)
                    .claims { it.putAll(claims) }
                    .expiresAt(expiresAt)
                    .subject(userId)
                    .audience(listOf(audience))
                    .notBefore(now)
                    .build())
        )

        return jwt
    }

    private fun parse(token: String, type: TokenType): Jwt {
        val claims = jwtDecoder.decode(token) ?: throw BadRequestException("Can't decode token")
        if (claims.claims["type"] != type.name) {
            throw ResponseStatusException(
                HttpStatusCode.valueOf(401),
                "Token is not a $type token."
            )
        }
        return claims
    }

    fun generateEmailVerificationLink(userId: String): String {
        val token = generateToken(
            audience = appProperties.baseUrl!!,
            userId = userId,
            claims = mapOf("type" to TokenType.VERIFY_EMAIL.name),
            validity = 1.days
        )
        return "${appProperties.baseUrl}/verify-email?token=${token?.tokenValue}"
    }

    fun retrieveClaimsIfValidEmailVerificationToken(token: String): Jwt {
        return parse(token, TokenType.VERIFY_EMAIL)
    }

    fun generateResetPasswordLink(userId: String): String {
        val token = generateToken(
            audience = appProperties.baseUrl!!,
            userId = userId,
            claims = mapOf("type" to TokenType.RESET_PASSWORD.name),
            validity = 1.days
        )
        return "${appProperties.baseUrl}/reset-password?token=${token?.tokenValue}"
    }


    fun retrieveClaimsIfValidResetPasswordToken(token: String): Jwt {
        return parse(token, TokenType.RESET_PASSWORD)
    }

    fun generateAccessToken(user: User): String {
        val scopes = listOf("openid", "profile", "email")
        val token = generateToken(
            audience = appProperties.baseUrl!!,
            userId = user.id.toHexString(),
            claims = mapOf(
                "type" to TokenType.ACCESS_TOKEN.name,
                "email" to user.email,
                "authorities" to user.roles.map { "ROLE_$it" },
                "givenName" to user.firstName,
                "familyName" to user.lastName,
                "scope" to scopes.joinToString(" "),
                "scp" to scopes
            ),
            validity = 1.hours
        )
        return token?.tokenValue ?: throw RuntimeException("Failed to generate access token")
    }

    fun validateAccessToken(token: String): String {
        val jwt = parse(token, TokenType.ACCESS_TOKEN)
        return jwt.subject ?: throw RuntimeException("Invalid token subject")
    }

    fun generateRefreshToken(user: User): String {
        val token = generateToken(
            audience = appProperties.baseUrl!!,
            userId = user.id.toHexString(),
            claims = mapOf(
                "type" to TokenType.REFRESH_TOKEN.name
            ),
            validity = 30.days
        )
        return token?.tokenValue ?: throw RuntimeException("Failed to generate refresh token")
    }

    fun validateRefreshToken(token: String): String {
        val jwt = parse(token, TokenType.REFRESH_TOKEN)
        return jwt.subject ?: throw RuntimeException("Invalid token subject")
    }

}
