package pitampoudel.komposeauth.core.utils

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie.ResponseCookieBuilder
import pitampoudel.komposeauth.app_config.service.AppConfigService

fun findServerUrl(request: HttpServletRequest): String {
    val scheme = request.scheme
    val port = request.serverPort
    val defaultPort = (scheme == "http" && port == 80) || (scheme == "https" && port == 443)
    val hostWithPort = if (defaultPort) request.serverName else "${request.serverName}:$port"
    return "$scheme://$hostWithPort"
}

fun ResponseCookieBuilder.configureDomain(appConfigService: AppConfigService): ResponseCookieBuilder =
    apply {
        val rpId = appConfigService.rpId()
        if (!rpId.isNullOrBlank() && isValidDomainFormat(rpId)) {
            domain(".$rpId")
        }
    }

private fun isValidDomainFormat(rpId: String): Boolean {
    // Defensive check: caller already validates !rpId.isNullOrBlank(), but we validate here too
    if (rpId.isEmpty()) return false

    // Domain should not start or end with a dot
    if (rpId.startsWith(".") || rpId.endsWith(".")) return false

    // Check each label (parts separated by dots)
    val labels = rpId.split(".")
    for (label in labels) {
        if (label.isEmpty()) return false // No consecutive dots
        if (label.startsWith("-") || label.endsWith("-")) return false // Hyphens at label boundaries
        // Each label should only contain alphanumeric and hyphens
        if (!label.all { it.isLetterOrDigit() || it == '-' }) return false
    }

    return true
}