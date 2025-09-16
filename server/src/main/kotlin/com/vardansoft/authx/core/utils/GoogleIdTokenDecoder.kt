package com.vardansoft.authx.core.utils

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import java.util.Collections

fun validateGoogleIdToken(clientId: String, idToken: String): GoogleIdToken.Payload {
    val verifier = GoogleIdTokenVerifier.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance()
    )
        .setAudience(Collections.singletonList(clientId))
        .build()

    val googleIdToken = verifier.verify(idToken)
        ?: throw IllegalArgumentException("Invalid Google ID token")

    return googleIdToken.payload
}
