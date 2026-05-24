package pitampoudel.komposeauth.core.utils

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie
import pitampoudel.komposeauth.app_config.service.AppConfigService

fun findServerUrl(request: HttpServletRequest): String {
    val scheme = request.scheme
    val port = request.serverPort
    val defaultPort = (scheme == "http" && port == 80) || (scheme == "https" && port == 443)
    val hostWithPort = if (defaultPort) request.serverName else "${request.serverName}:$port"
    return "$scheme://$hostWithPort"
}

fun ResponseCookie.Builder.configureDomain(appConfigService: AppConfigService): ResponseCookie.Builder =
    apply {
        val rpId = appConfigService.rpId()
        if (!rpId.isNullOrBlank()) {
            domain(".$rpId")
        }
    }