package pitampoudel.komposeauth.core.security

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames

/**
 * Refuses a JWT shaped like an ID token. One is signed with the same key as an access token, names
 * the same user and, for a client registered here, carries the same audience, so nothing else tells
 * the two apart; but an ID token is handed to the app to read and travels where a bearer must not
 * (`id_token_hint` in a URL, logs).
 *
 * The two kinds are told apart by claims each is always written with and the other never is: both
 * places that mint an access token, the authorization server's generator and `/login`, write `nbf`,
 * and the generator writes `azp` on an ID token and on nothing else. That holds for every access
 * token already issued, which a new marker claim would not. `scope` was the other candidate, and is
 * absent from an access token whose grant asked for no scope.
 */
object AccessTokenShape : OAuth2TokenValidator<Jwt> {
    private val notAnAccessToken = OAuth2Error(
        OAuth2ErrorCodes.INVALID_TOKEN,
        "An ID token is not a bearer; send the access token",
        null
    )

    override fun validate(token: Jwt): OAuth2TokenValidatorResult =
        if (token.hasClaim(JwtClaimNames.NBF) && !token.hasClaim(IdTokenClaimNames.AZP)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(notAnAccessToken)
        }
}
