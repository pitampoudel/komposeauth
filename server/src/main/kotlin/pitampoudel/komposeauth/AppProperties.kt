package pitampoudel.komposeauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import pitampoudel.komposeauth.domain.Platform
import pitampoudel.komposeauth.setup.service.AppConfigService
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
class AppProperties {

    @Autowired
    lateinit var appConfigService: AppConfigService

    var selfBaseUrl: String = ""
        get() {
            val raw = field.trim()
            if (raw.isNotEmpty()) return raw
            val cfg = appConfigService.get().selfBaseUrl?.trim()
            if (!cfg.isNullOrBlank()) return cfg
            return "http://${getLocalIpAddress()}:8080"
        }

    var name: String = ""
        get() = field.takeIf { it.isNotBlank() } ?: appConfigService.get().name ?: "komposeauth"
    var logoUrl: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().logoUrl
    var expectedGcpProjectId: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().expectedGcpProjectId
    var gcpBucketName: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().gcpBucketName
    var googleAuthClientId: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().googleAuthClientId
    var googleAuthClientSecret: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().googleAuthClientSecret
    var googleAuthDesktopClientId: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: appConfigService.get().googleAuthDesktopClientId
    var googleAuthDesktopClientSecret: String? = null
        get() = field?.takeIf { it.isNotBlank() }
            ?: appConfigService.get().googleAuthDesktopClientSecret
    var assetLinksJson: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().assetLinksJson
    var twilioAccountSid: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().twilioAccountSid
    var twilioAuthToken: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().twilioAuthToken
    var twilioFromNumber: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().twilioFromNumber
    var twilioVerifyServiceSid: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().twilioVerifyServiceSid
    var smtpHost: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().smtpHost
    var smtpPort: Int? = null
        get() = field ?: appConfigService.get().smtpPort
    var smtpUsername: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().smtpUsername
    var smtpPassword: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().smtpPassword
    var smtpFromEmail: String? = null
        get() = field?.takeIf { it.isNotBlank() } ?: appConfigService.get().smtpFromEmail

    var samayeApiKey: String? = null

    fun rpId(): String = URL(selfBaseUrl).host

    fun assetLinks(): List<AssetLink>? {
        return assetLinksJson?.let { Json.decodeFromString<List<AssetLink>>(it) }
    }

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