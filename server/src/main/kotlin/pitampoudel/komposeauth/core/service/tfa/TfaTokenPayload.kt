package pitampoudel.komposeauth.core.service.tfa

import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant


@Service
class JwtTokenService {
    fun generateHs256Token(
        subject: String,
        issuer: String,
        claims: Map<String, String>
    ): String {
        val claims = JwtClaimsSet.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(Instant.now())
            .also {
                claims.forEach { (key, value) -> it.claim(key, value) }
            }

            .build()

        return JwtEncoder().encode(JwtEncoderParameters.from(claims)).tokenValue
    }
}

public fun main() {
    val jwt = JwtTokenService().generateHs256Token(
        subject = "user-1",
        issuer = "https://example.com",
        claims = mapOf(
            "name" to "",
            "token" to "",
            "identifier" to "",
            "label" to "",
            "secondary_label" to "",
            "callback" to "",
            "is_sdk" to "",
        )
    )
    println(jwt)
}