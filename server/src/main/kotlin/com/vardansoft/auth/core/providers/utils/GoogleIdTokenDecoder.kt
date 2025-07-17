package com.vardansoft.auth.core.providers.utils

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory


fun validateGoogleIdToken(clientId: String, idToken: String): GoogleIdToken.Payload {
    val verifier = GoogleIdTokenVerifier.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance()
    )
        .setAudience(listOf(clientId))
        .build()

    val token = verifier.verify(idToken)
    if (token != null) {
        return token.payload
    }
    throw SecurityException("Invalid Google token")
}
