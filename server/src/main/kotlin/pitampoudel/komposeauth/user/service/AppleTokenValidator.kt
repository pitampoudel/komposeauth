package pitampoudel.komposeauth.user.service

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Component
import java.net.URL
import java.text.ParseException
import java.util.Date

@Component
class AppleTokenValidator {

    private val applePublicKeyUrl = "https://appleid.apple.com/auth/keys"

    fun validate(idToken: String, clientId: String): JWTClaimsSet {
        val signedJwt = try {
            SignedJWT.parse(idToken.trim())
        } catch (e: ParseException) {
            throw IllegalArgumentException(
                "Invalid Apple ID token $idToken. (expected JWT: header.payload.signature)",
                e
            )
        }
        val jwkSet = JWKSet.load(URL(applePublicKeyUrl))
        val jwk = jwkSet.getKeyByKeyId(signedJwt.header.keyID)
            ?: throw IllegalArgumentException("Could not find matching public key")

        val jwsVerifier = com.nimbusds.jose.crypto.RSASSAVerifier(jwk.toRSAKey())

        if (!signedJwt.verify(jwsVerifier)) {
            throw IllegalArgumentException("Invalid token signature")
        }

        val claims = signedJwt.jwtClaimsSet
        val issuer = claims.issuer
        if (issuer != "https://appleid.apple.com") {
            throw IllegalArgumentException("Invalid issuer")
        }

        val audience = claims.audience
        if (!audience.contains(clientId)) {
            throw IllegalArgumentException("Invalid audience")
        }

        val expirationTime = claims.expirationTime
        if (expirationTime.before(Date())) {
            throw IllegalArgumentException("Token expired")
        }

        return claims
    }
}
