package pitampoudel.komposeauth.core.service.jwt

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*


@Service
class JwtTokenService {
    fun generateHs256Token(
        secretKey: String,
        subject: String,
        issuer: String,
        claims: Map<String, String>
    ): String {
        val claimsSet = JwtClaimsSet.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(Instant.now())
            .also { builder ->
                claims.forEach { (key, value) -> builder.claim(key, value) }
            }
            .build()

        val encoder = NimbusJwtEncoder(
            ImmutableJWKSet(
                JWKSet(
                    OctetSequenceKey.Builder(
                        Base64.getDecoder().decode(secretKey)
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
}

