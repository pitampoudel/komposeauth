package pitampoudel.komposeauth.core.utils

import jakarta.servlet.http.HttpServletRequest

fun findCurrentBaseUrl(request: HttpServletRequest): String {
    val scheme = request.scheme
    val port = request.serverPort
    val defaultPort = (scheme == "http" && port == 80) || (scheme == "https" && port == 443)
    val hostWithPort = if (defaultPort) request.serverName else "${request.serverName}:$port"
    val contextPath = request.contextPath ?: ""
    return "$scheme://$hostWithPort$contextPath"
}