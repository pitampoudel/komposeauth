package pitampoudel.komposeauth.user.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Verifies an ID token from Sign in with Apple: signature, issuer, expiry and audience. Apple's keys
 * are cached by the decoder rather than fetched on every sign-in, and fetched with the timeouts the
 * shared [RestTemplate] carries.
 */
@Component
class AppleTokenValidator(restTemplate: RestTemplate) {

    private val decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_KEYS_URL)
        .restOperations(restTemplate)
        .build()
        .apply { setJwtValidator(JwtValidators.createDefaultWithIssuer(APPLE_ISSUER)) }

    fun validate(idToken: String, clientId: String): Jwt {
        val jwt = try {
            decoder.decode(idToken.trim())
        } catch (_: BadJwtException) {
            throw AccessDeniedException("Invalid credentials")
        }
        if (clientId !in jwt.audience) throw AccessDeniedException("Invalid credentials")
        return jwt
    }

    private companion object {
        const val APPLE_ISSUER = "https://appleid.apple.com"
        const val APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys"
    }
}
