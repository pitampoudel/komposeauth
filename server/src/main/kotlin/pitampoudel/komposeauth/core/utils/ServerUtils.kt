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

/**
 * Configures the cookie domain based on the configured rpId (relying party identifier).
 *
 * When rpId is configured in AppConfig, this extension function sets the cookie domain to
 * `.{rpId}` format (e.g., `.godaan.com.np`), which is RFC 6265 compliant and allows the
 * cookie to be shared across all subdomains of the specified domain (e.g., api.godaan.com.np,
 * web.godaan.com.np).
 *
 * The leading dot in the domain format is significant - it indicates that the cookie applies
 * to the domain and all its subdomains, following RFC 6265 specifications.
 *
 * If rpId is not configured or is blank, the cookie domain is not set, allowing default
 * behavior (the cookie applies only to the exact domain that set it).
 *
 * @param appConfigService The service providing configuration including rpId
 * @return The builder itself for chaining
 *
 * @see <a href="https://tools.ietf.org/html/rfc6265#section-5.1.3">RFC 6265 - Domain Attribute</a>
 */
fun ResponseCookie.Builder.configureDomain(appConfigService: AppConfigService): ResponseCookie.Builder =
    apply {
        val rpId = appConfigService.rpId()
        if (!rpId.isNullOrBlank() && isValidDomainFormat(rpId)) {
            domain(".$rpId")
        }
    }

/**
 * Validates that rpId is in a valid domain format per RFC 1035 and RFC 6265.
 * Checks for invalid characters and structural issues that could result in a malformed cookie domain.
 */
private fun isValidDomainFormat(rpId: String): Boolean {
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