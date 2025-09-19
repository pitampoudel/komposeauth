package com.vardansoft.authx.ui.login

import com.sun.net.httpserver.HttpServer
import com.vardansoft.authx.data.Credential
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection

object GoogleAuthPKCE {

    fun getCredential(clientId: String): Credential? {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallengeS256(codeVerifier)
        val state = randomString(32)

        // Bind to a random available port on loopback to follow Google Desktop OAuth best practices
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var authCode: String? = null
        var returnedState: String? = null
        val lock = Object()

        server.createContext("/callback") { exchange ->
            val query = exchange.requestURI.query
            authCode = query.substringAfter("code=").substringBefore("&")
            returnedState = query.substringAfter("state=").substringBefore("&")
            val response = "Authentication successful. You may close this window."
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            synchronized(lock) { lock.notify() }
        }
        server.start()

        // Compute redirect URI with actual assigned port
        val redirectUri = "http://127.0.0.1:${server.address.port}/callback"

        // Build auth URL with PKCE
        val params = mapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to "openid email profile",
            "access_type" to "offline",
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
            "state" to state
        )
        val queryString = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
        }
        val authUri = URI.create("https://accounts.google.com/o/oauth2/v2/auth?${queryString}")

        // Open in browser
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(authUri)
        } else {
            println("Open this URL: $authUri")
        }

        // Wait until callback received
        synchronized(lock) { lock.wait() }
        server.stop(0)

        val code = authCode ?: return null
        if (state != returnedState) {
            println("State mismatch in OAuth callback. Potential CSRF detected.")
            return null
        }

        // Exchange code for tokens at Google token endpoint using PKCE (no client secret)
        val tokenUrl = URL("https://oauth2.googleapis.com/token")
        val bodyParams = mapOf(
            "client_id" to clientId,
            "code" to code,
            "code_verifier" to codeVerifier,
            "grant_type" to "authorization_code",
            "redirect_uri" to redirectUri
        )
        val body = bodyParams.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
        }

        val conn = tokenUrl.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

        val responseText = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            println("Token exchange failed: ${e.message}. Response: ${err}")
            return null
        } finally {
            conn.disconnect()
        }

        val json = Json.parseToJsonElement(responseText).jsonObject
        val idToken = json["id_token"]?.jsonPrimitive?.content
        if (idToken.isNullOrBlank()) {
            println("No id_token received from Google token endpoint.")
            return null
        }
        return Credential.GoogleId(idToken)
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        return base64UrlEncodeNoPadding(code)
    }

    private fun generateCodeChallengeS256(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return base64UrlEncodeNoPadding(digest)
    }

    private fun base64UrlEncodeNoPadding(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun randomString(length: Int): String {
        val alphabet = (('a'..'z') + ('A'..'Z') + ('0'..'9')).joinToString("")
        val rnd = SecureRandom()
        return (1..length).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
    }
}
