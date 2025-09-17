package com.vardansoft.authx.ui.login

import com.sun.net.httpserver.HttpServer
import com.vardansoft.authx.data.Credential
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI

object GoogleAuthPKCE {
    fun getCredential(clientId: String): Credential? {
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

        return Credential.GooglePKCEAuthCode(
            authCode = authCode ?: return null
        )
    }
}
