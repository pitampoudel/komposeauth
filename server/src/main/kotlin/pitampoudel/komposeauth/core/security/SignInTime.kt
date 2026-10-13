package pitampoudel.komposeauth.core.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.FactorGrantedAuthority
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import pitampoudel.komposeauth.user.data.Credential
import java.time.Instant

/** The access-token claim saying when the person signed in, as OIDC names it on an ID token. */
const val AUTH_TIME_CLAIM = IdTokenClaimNames.AUTH_TIME

/**
 * When the person behind this authentication last proved who they are, or null when nothing says.
 *
 * A bearer says so in [AUTH_TIME_CLAIM], which only a sign-in writes: a refreshed token leaves it
 * out (`/login`) or carries the sign-in's (`/oauth2/token`). Spring's own bearer factor is not it,
 * being stamped with the time of the request. A session says so in the factors its sign-in recorded.
 */
fun Authentication.signedInAt(): Instant? = when (this) {
    is JwtAuthenticationToken -> token.getClaimAsInstant(AUTH_TIME_CLAIM)
    else -> signInFactors().maxOfOrNull { it.issuedAt }
}

/** The factors a sign-in recorded on this authentication, each with when it was proven. */
fun Authentication.signInFactors(): List<FactorGrantedAuthority> =
    authorities.filterIsInstance<FactorGrantedAuthority>()
        .filterNot { it.authority == FactorGrantedAuthority.BEARER_AUTHORITY }

/** The factor this credential proves, or null for a refresh token, which proves none. */
fun Credential.signInFactor(): String? = when (this) {
    is Credential.UsernamePassword -> FactorGrantedAuthority.PASSWORD_AUTHORITY
    is Credential.OTP -> FactorGrantedAuthority.OTT_AUTHORITY
    is Credential.PublicKey -> FactorGrantedAuthority.WEBAUTHN_AUTHORITY
    is Credential.GoogleId, is Credential.AppleId, is Credential.AuthCode -> FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY
    is Credential.RefreshToken -> null
}
