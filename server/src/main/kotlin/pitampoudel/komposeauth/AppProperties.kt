package pitampoudel.komposeauth

import com.google.common.net.InternetDomainName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import pitampoudel.komposeauth.domain.Platform
import pitampoudel.komposeauth.setup.service.EnvService
import pitampoudel.komposeauth.webauthn.utils.WebAuthnUtils.androidOrigin
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

@Serializable
data class AssetLink(
    @SerialName("relation")
    val relation: List<String>,
    @SerialName("target")
    val target: JsonObject
)

@Configuration
@ConfigurationProperties(prefix = "app")
class AppProperties(val envService: EnvService) {

    val domain: String
        get() {
            val host = runCatching { URL(selfBaseUrl).host }.getOrNull() ?: return selfBaseUrl
            if (!InternetDomainName.isValid(host)) return host
            val domainName = InternetDomainName.from(host)
            return if (domainName.isUnderPublicSuffix) {
                domainName.topPrivateDomain().toString()
            } else host
        }


    var selfBaseUrl: String = ""
        get() {
            val raw = field.trim()
            if (raw.isNotEmpty()) return raw
            val cfg = envService.getEnv().selfBaseUrl?.trim()
            if (!cfg.isNullOrBlank()) return cfg
            return "http://${getLocalIpAddress()}:8080"
        }

    var name: String = ""
        get() = field.takeIf { it.isNotBlank() }
            ?: envService.getEnv().name.takeIf { !it.isNullOrBlank() }
            ?: "komposeauth"

    var logoUrl: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().logoUrl.takeIf { !it.isNullOrBlank() }
    var expectedGcpProjectId: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().expectedGcpProjectId.takeIf { !it.isNullOrBlank() }
    var gcpBucketName: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().gcpBucketName.takeIf { !it.isNullOrBlank() }
    var googleAuthClientId: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().googleAuthClientId.takeIf { !it.isNullOrBlank() }
    var googleAuthClientSecret: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().googleAuthClientSecret.takeIf { !it.isNullOrBlank() }
    var googleAuthDesktopClientId: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().googleAuthDesktopClientId.takeIf { !it.isNullOrBlank() }
    var googleAuthDesktopClientSecret: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().googleAuthDesktopClientSecret.takeIf { !it.isNullOrBlank() }
    var androidSha256List: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().androidSha256List.takeIf { !it.isNullOrBlank() }
    var twilioAccountSid: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().twilioAccountSid.takeIf { !it.isNullOrBlank() }
    var twilioAuthToken: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().twilioAuthToken.takeIf { !it.isNullOrBlank() }
    var twilioFromNumber: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().twilioFromNumber.takeIf { !it.isNullOrBlank() }
    var twilioVerifyServiceSid: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().twilioVerifyServiceSid.takeIf { !it.isNullOrBlank() }
    var smtpHost: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().smtpHost.takeIf { !it.isNullOrBlank() }
    var smtpPort: Int? = null
        get() = field ?: envService.getEnv().smtpPort
    var smtpUsername: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().smtpUsername.takeIf { !it.isNullOrBlank() }
    var smtpPassword: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().smtpPassword.takeIf { !it.isNullOrBlank() }
    var smtpFromEmail: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().smtpFromEmail.takeIf { !it.isNullOrBlank() }

    var sentryDsn: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: envService.getEnv().sentryDsn.takeIf { !it.isNullOrBlank() }

    var samayeApiKey: String? = null
    fun googleClientId(platform: Platform): String? {
        val value = when (platform) {
            Platform.DESKTOP -> googleAuthDesktopClientId
            Platform.WEB -> googleAuthClientId
            Platform.ANDROID -> googleAuthClientId
            Platform.IOS -> googleAuthClientId
        }
        return value?.takeIf { it.isNotBlank() }
    }

    fun googleClientSecret(platform: Platform): String? {
        val value = when (platform) {
            Platform.DESKTOP -> googleAuthDesktopClientSecret
            Platform.WEB -> googleAuthClientSecret
            Platform.ANDROID -> googleAuthClientSecret
            Platform.IOS -> googleAuthClientSecret
        }
        return value?.takeIf { it.isNotBlank() }
    }

    fun androidOrigins(): Set<String> {
        return androidSha256List?.split(",")?.map {
            androidOrigin(it)
        }.orEmpty().toSet()
    }

    fun getLocalIpAddress(): String? {
        val excludePrefixes = listOf(
            "veth", "docker", "virbr", "br-", "tun", "tap", "ppp", "vpn", "vmnet", "vboxnet"
        )

        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .sortedBy { it.index } // lower index usually means physical NIC

        for (ni in interfaces) {
            val name = ni.name.lowercase()
            if (excludePrefixes.any { name.startsWith(it) }) continue

            for (addr in Collections.list(ni.inetAddresses)) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

}
