package pitampoudel.komposeauth.core.service.jwt

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.crypto.spec.SecretKeySpec


@Service
class JwtTokenService {

    companion object {
        /** Long enough to finish a KYC session, short enough that a leaked token goes stale fast. */
        private val DEFAULT_TTL: Duration = Duration.ofMinutes(30)
    }
    fun generateHs256Token(
        secretKey: String,
        subject: String,
        issuer: String,
        claims: Map<String, String>,
        ttl: Duration = DEFAULT_TTL
    ): String {
        val secretBytes = resolveHs256Secret(secretKey)
        val issuedAt = Instant.now()
        val claimsSet = JwtClaimsSet.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(issuedAt)
            // Without an `exp` claim these tokens are valid forever, and the endpoint that accepts
            // them is public — one leaked token would stay replayable indefinitely.
            .expiresAt(issuedAt.plus(ttl))
            .also { builder ->
                claims.forEach { (key, value) -> builder.claim(key, value) }
            }
            .build()

        val encoder = NimbusJwtEncoder(
            ImmutableJWKSet(
                JWKSet(
                    OctetSequenceKey.Builder(
                        secretBytes
                    ).build()
                )
            )
        )
        return encoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claimsSet
            )
        ).tokenValue
    }

    /** Returns the verified claims so callers can bind on them rather than on untrusted request data. */
    fun verifyHs256Token(token: String, secretKey: String): Jwt {
        val secretBytes = resolveHs256Secret(secretKey)
        val decoder = NimbusJwtDecoder
            .withSecretKey(SecretKeySpec(secretBytes, "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtTimestampValidator(),
                // A token minted before this change carries no `exp`; the timestamp validator lets
                // those through, so require the claim explicitly.
                JwtClaimValidator<Instant?>(JwtClaimNames.EXP) { it != null }
            )
        )
        return decoder.decode(token)
    }

    private fun resolveHs256Secret(secretKey: String): ByteArray {
        val trimmed = secretKey.trim()
        val decoded = runCatching { Base64.getDecoder().decode(trimmed) }.getOrNull()
        val secretBytes = when {
            decoded != null && decoded.size >= 32 -> decoded
            trimmed.toByteArray(Charsets.UTF_8).size >= 32 -> trimmed.toByteArray(Charsets.UTF_8)
            else -> decoded ?: trimmed.toByteArray(Charsets.UTF_8)
        }
        require(secretBytes.size >= 32) {
            "HS256 requires a secret of at least 256 bits (32 bytes) after decoding."
        }
        return secretBytes
    }
}


