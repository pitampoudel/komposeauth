package pitampoudel.komposeauth.core.service

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.AppProperties
import pitampoudel.komposeauth.user.entity.User
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Service
class JwtService(
    val appProperties: AppProperties,
    private val jwtEncoder: JwtEncoder
) {

    private fun generateToken(
        audience: String,
        userId: String,
        claims: Map<String, Any?> = mapOf(),
        validity: Duration,
    ): Jwt? {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(validity.inWholeSeconds)

        val jwt = jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwtClaimsSet.builder()
                    .issuer(appProperties.selfBaseUrl)
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

    fun generateAccessToken(user: User): String {
        val scopes = listOf("openid", "profile", "email")
        val token = generateToken(
            audience = appProperties.selfBaseUrl,
            userId = user.id.toHexString(),
            claims = mapOf(
                "email" to user.email,
                "authorities" to user.roles.map { "ROLE_$it" },
                "givenName" to user.firstName,
                "familyName" to user.lastName,
                "scope" to scopes.joinToString(" "),
                "scp" to scopes
            ),
            validity = 1.days
        )
        return token?.tokenValue ?: throw RuntimeException("Failed to generate access token")
    }
}
