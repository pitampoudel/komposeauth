package pitampoudel.komposeauth.core.utils

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import pitampoudel.komposeauth.user.data.CreateUserRequest
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies a raw Google ID token: signature, issuer, expiry, and audience against [clientIds].
 *
 * Only for tokens this server has not already had verified for it — the ones a native client sends
 * to the login API. A token that arrived through the browser sign-in flow has been through all of
 * this already, inside Spring Security's OIDC login, against a key set it caches per registration.
 *
 * The verifier is held rather than rebuilt because it owns the cache of Google's signing
 * certificates. A fresh one per call has an empty cache, so every sign-in became a live fetch of
 * `https://www.googleapis.com/oauth2/v3/certs` — one slow or failed fetch, and a perfectly good
 * sign-in failed. Google's own guidance is to reuse the instance; it is documented thread-safe. The
 * audience is fixed when the verifier is built, so a configuration change rebuilds it.
 */
fun validateGoogleIdToken(clientIds: List<String>, idToken: String): GoogleIdToken.Payload {
    val googleIdToken = verifierFor(clientIds).verify(idToken)
        ?: throw IllegalArgumentException("Invalid Google ID token")

    return googleIdToken.payload
}

private val cachedVerifier = AtomicReference<Pair<List<String>, GoogleIdTokenVerifier>?>(null)

private fun verifierFor(clientIds: List<String>): GoogleIdTokenVerifier {
    cachedVerifier.get()?.let { (audience, verifier) ->
        if (audience == clientIds) return verifier
    }
    val verifier = GoogleIdTokenVerifier.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance()
    )
        .setAudience(clientIds)
        .build()
    cachedVerifier.set(clientIds to verifier)
    return verifier
}

/**
 * Reads a verified Google payload into the shape this application creates users from.
 *
 * Separate from the verification above so it can be exercised without a network round trip, which
 * matters because the interesting cases are all about claims that are *missing*. Only `sub` and
 * `email` can be relied on: Google documents the name claims as appearing "when a name claim is
 * present", so an account with no name set, or one whose directory holds a single display name,
 * arrives without them. Reading those as non-null threw an NPE inside the sign-in filter, where no
 * exception handler could reach it, and the visitor got a whitelabel 500 on the callback.
 */
fun googleProfileFrom(payload: GoogleIdToken.Payload): CreateUserRequest =
    googleProfileFromClaims(payload)

/**
 * The same reading, from claims somebody else verified — the browser sign-in flow hands over what
 * Spring Security's OIDC login has already checked, so nothing here needs to re-derive it.
 */
fun googleProfileFromClaims(claims: Map<String, Any?>): CreateUserRequest {
    val email = claims["email"] as? String
        ?: throw IllegalArgumentException("Google did not return an email address for this account")

    return CreateUserRequest(
        email = email,
        // Falls back to the display name, so an account carrying only that still gets something to
        // be called rather than nothing at all.
        firstName = (claims["given_name"] as? String) ?: (claims["name"] as? String),
        lastName = claims["family_name"] as? String,
        photoUrl = claims["picture"] as? String
    )
}
