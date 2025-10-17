package com.vardansoft.komposeauth.core.utils

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

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
