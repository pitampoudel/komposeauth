package pitampoudel.komposeauth.core.utils

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.stereotype.Component

/**
 * This server's public address: every token's issuer, every emailed link and the KYC callback are
 * built from it. It is `spring.security.oauth2.authorizationserver.issuer`, which the authorization
 * server reads too. Left unset, it is read off each request, whose Host and forwarded headers the
 * caller writes.
 */
@Component
class ServerUrl(settings: AuthorizationServerSettings) {
    private val issuer: String? = settings.issuer

    init {
        if (issuer == null) {
            LoggerFactory.getLogger(javaClass).warn(
                "spring.security.oauth2.authorizationserver.issuer is not set: token issuers and " +
                    "emailed links follow each request's Host header"
            )
        }
    }

    fun of(request: HttpServletRequest): String = issuer ?: fromRequest(request)

    private fun fromRequest(request: HttpServletRequest): String {
        val scheme = request.scheme
        val port = request.serverPort
        val defaultPort = (scheme == "http" && port == 80) || (scheme == "https" && port == 443)
        val hostWithPort = if (defaultPort) request.serverName else "${request.serverName}:$port"
        return "$scheme://$hostWithPort"
    }
}

/** The one spelling an email address is stored and looked up under. */
fun String.normalizedEmail(): String = trim().lowercase()
