package com.vardansoft.authx.core.service

import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Service
class JwtService(
    @Value("\${app.baseUrl}")
    val baseUrl: String,
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: JwtDecoder
) {
    enum class TokenType {
        VERIFY_EMAIL,
        RESET_PASSWORD
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
                    .issuer(baseUrl)
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
            audience = baseUrl,
            userId = userId,
            claims = mapOf("type" to TokenType.VERIFY_EMAIL.name),
            validity = 1.days
        )
        return "${baseUrl}/verify-email?token=$token"
    }

    fun generateResetPasswordLink(userId: String): String {
        val token = generateToken(
            audience = baseUrl,
            userId = userId,
            claims = mapOf("type" to TokenType.RESET_PASSWORD.name),
            validity = 1.days
        )
        return "${baseUrl}/reset-password?token=$token"
    }


    fun retrieveClaimsIfValidEmailVerificationToken(token: String): Jwt {
        return parse(token, TokenType.VERIFY_EMAIL)
    }

    fun retrieveClaimsIfValidResetPasswordToken(token: String): Jwt {
        return parse(token, TokenType.RESET_PASSWORD)
    }

}
