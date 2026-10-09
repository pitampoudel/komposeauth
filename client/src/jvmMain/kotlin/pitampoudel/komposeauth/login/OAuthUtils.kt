package pitampoudel.komposeauth.login

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

object OAuthUtils {

    fun buildAuthUrl(clientId: String, redirectUri: String, state: String): String =
        "https://accounts.google.com/o/oauth2/v2/auth?" + listOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to "openid email profile",
            "state" to state,
            "prompt" to "select_account"
        ).joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, UTF_8)}" }

    /**
     * Receives Google's redirect on the loopback interface only, on a port the system picks and this
     * server already holds, so nothing else on the machine or the network can answer in its place.
     */
    class LoopbackReceiver : AutoCloseable {
        private val server = HttpServer.create(InetSocketAddress(InetAddress.getByName(LOOPBACK), 0), 0)
        private val received = CompletableDeferred<Map<String, String>>()

        val redirectUri = "http://$LOOPBACK:${server.address.port}/callback"

        init {
            server.createContext("/callback") { exchange ->
                val params = exchange.requestURI.rawQuery.orEmpty()
                    .split("&")
                    .filter { it.isNotEmpty() }
                    .associate { pair ->
                        val key = pair.substringBefore("=")
                        val value = pair.substringAfter("=", "")
                        URLDecoder.decode(key, UTF_8) to URLDecoder.decode(value, UTF_8)
                    }
                val response = "You can close this window now.".toByteArray()
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.use { it.write(response) }
                received.complete(params)
            }
            server.start()
        }

        /** The query parameters of the redirect: `code` and `state`, or `error` when the user declined. */
        suspend fun awaitRedirect(): Map<String, String> = received.await()

        override fun close() = server.stop(0)
    }

    private const val LOOPBACK = "127.0.0.1"
}
