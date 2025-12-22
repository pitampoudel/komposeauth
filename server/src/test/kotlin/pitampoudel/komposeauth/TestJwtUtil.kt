package pitampoudel.komposeauth

import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

/**
 * Helpers for building [Jwt] instances for tests without involving signing.
 */
object TestJwtUtil {
    fun jwt(
        subject: String = "user-id",
        authorities: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
        issuedAt: Instant = Instant.now().minusSeconds(10),
        expiresAt: Instant = Instant.now().plusSeconds(3600),
        extraClaims: Map<String, Any> = emptyMap()
    ): Jwt {
        val claims = mutableMapOf(
            "sub" to subject,
            "authorities" to authorities,
            "scope" to scopes
        )
        claims.putAll(extraClaims)

        return Jwt(
            "test-token",
            issuedAt,
            expiresAt,
            mapOf("alg" to "none"),
            claims
        )
    }
}

