package pitampoudel.komposeauth.core.utils

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import pitampoudel.komposeauth.user.data.CreateUserRequest

fun validateGoogleIdToken(clientIds: List<String>, idToken: String): GoogleIdToken.Payload {
    val verifier = GoogleIdTokenVerifier.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance()
    )
        .setAudience(clientIds)
        .build()

    val googleIdToken = verifier.verify(idToken)
        ?: throw IllegalArgumentException("Invalid Google ID token")

    return googleIdToken.payload
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
fun googleProfileFrom(payload: GoogleIdToken.Payload): CreateUserRequest {
    val email = payload["email"] as? String
        ?: throw IllegalArgumentException("Google did not return an email address for this account")

    return CreateUserRequest(
        email = email,
        // Falls back to the display name, so an account carrying only that still gets something to
        // be called rather than nothing at all.
        firstName = (payload["given_name"] as? String) ?: (payload["name"] as? String),
        lastName = payload["family_name"] as? String,
        photoUrl = payload["picture"] as? String
    )
}
