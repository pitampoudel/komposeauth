package pitampoudel.komposeauth.core.service

import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.config.service.AppConfigProvider
import pitampoudel.komposeauth.user.entity.User
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@Service
class JwtService(
    val appConfigProvider: AppConfigProvider,
    private val jwtEncoder: JwtEncoder
) {
    fun generateAccessToken(user: User, validity: Duration): String {
        val now = Instant.now()
        val scopes = listOf("openid", "profile", "email")
        val claims = JwtClaimsSet.builder()
            .issuer(appConfigProvider.selfBaseUrl)
            .subject(user.id.toHexString())
            .audience(listOf(appConfigProvider.selfBaseUrl))
            .issuedAt(now)
            .expiresAt(now + validity.toJavaDuration())
            .notBefore(now)
            .claim("email", user.email)
            .claim("givenName", user.firstName)
            .claim("familyName", user.lastName)
            .claim("authorities", user.roles.map { "ROLE_$it" })
            .claim("scope", scopes.joinToString(" "))
            .claim("scp", scopes)
            .build()
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
    }
}
