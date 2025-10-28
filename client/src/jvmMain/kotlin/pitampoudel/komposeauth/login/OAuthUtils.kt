package pitampoudel.komposeauth.login

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.Base64
import java.util.Random
import kotlin.text.Charsets.UTF_8

object OAuthUtils {
    fun generateCodeVerifier(): String {
        val random = ByteArray(32)
        Random().nextBytes(random)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random)
    }

    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun buildAuthUrl(clientId: String, redirectUri: String, codeChallenge: String): String {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=$clientId" +
                "&redirect_uri=$redirectUri" +
                "&response_type=code" +
                "&scope=openid%20email%20profile" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256" +
                "&access_type=offline"
    }

    fun listenForCode(port: Int): String {
        var authCode: String? = null
        val server = HttpServer.create(InetSocketAddress(port), 0)
        val lock = Object()

        server.createContext("/callback") { exchange ->
            val query = exchange.requestURI.query
            val params = query.split("&").associate {
                val (k, v) = it.split("=")
                k to v
            }
            authCode = params["code"]

            val response = "You can close this window now."
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            synchronized(lock) { lock.notify() }
        }

        server.start()
        synchronized(lock) { lock.wait() }
        server.stop(0)

        return authCode ?: throw IllegalStateException("No code received")
    }

}