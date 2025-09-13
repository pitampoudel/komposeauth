package com.vardansoft.authx.ui.login

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import java.awt.Desktop
import java.net.URI

object GoogleAuth {
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    fun getCredential(clientId: String): String {
        val receiver = LocalServerReceiver.Builder().setPort(8080).build()
        val redirectUri = receiver.redirectUri

        val clientSecrets = GoogleClientSecrets()
            .setInstalled(
                GoogleClientSecrets
                    .Details()
                    .setClientId(clientId)
                    .setRedirectUris(listOf(redirectUri))
            )

        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            jsonFactory,
            clientSecrets,
            listOf("openid", "email", "profile")
        ).setAccessType("offline").build()

        try {
            // Build authorization URL and open in browser
            val authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri)
            if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                    .isSupported(Desktop.Action.BROWSE)
            ) {
                Desktop.getDesktop().browse(URI(authorizationUrl.build()))
            } else {
                println("Please open this URL in a browser: ${authorizationUrl.build()}")
            }

            // Wait for the authorization code
            val code = receiver.waitForCode()

            // Exchange code for tokens
            val response = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute() as GoogleTokenResponse

            // Store credentials
            flow.createAndStoreCredential(response, "user")

            return response.idToken
        } finally {
            receiver.stop()
        }
    }
}
