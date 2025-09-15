package com.vardansoft.authx.ui.login

import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object GoogleAuthPKCE {
    private fun generateCodeVerifier(): String {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun getCredential(clientId: String): String? {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 8080), 0)
        var authCode: String? = null
        val lock = Object()

        server.createContext("/callback") { exchange ->
            val query = exchange.requestURI.query
            authCode = query.substringAfter("code=").substringBefore("&")
            val response = "Authentication successful. You may close this window."
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            synchronized(lock) { lock.notify() }
        }
        server.start()

        // Build auth URL
        val authUri = URI.create(
            "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=$clientId&" +
                    "redirect_uri=http://127.0.0.1:8080/callback&" +
                    "response_type=code&" +
                    "scope=openid%20email%20profile&" +
                    "code_challenge=$challenge&" +
                    "code_challenge_method=S256&" +
                    "access_type=offline"
        )

        // Open in browser
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(authUri)
        } else {
            println("Open this URL: $authUri")
        }

        // Wait until callback received
        synchronized(lock) { lock.wait() }
        server.stop(0)

        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "code=$authCode&" +
                                "client_id=$clientId&" +
                                "redirect_uri=http://127.0.0.1:8080/callback&" +
                                "grant_type=authorization_code&" +
                                "code_verifier=$verifier"
                    )
                )
                .build(), HttpResponse.BodyHandlers.ofString()
        )
        val json = Json.decodeFromString<JsonObject>(response.body())
        println(json)
        return json["id_token"]?.jsonPrimitive?.content
    }
}
