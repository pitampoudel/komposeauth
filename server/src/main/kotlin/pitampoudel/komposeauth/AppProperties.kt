package pitampoudel.komposeauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import pitampoudel.komposeauth.domain.Platform
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
    var selfBaseUrl: String = ""
        get() {
            val raw = field.trim()
            if (raw.isNotEmpty()) return raw
            return "http://${getLocalIpAddress()}:8080"
        }

    var gcpBucketName: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    lateinit var name: String
    var logoUrl: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var expectedGcpProjectId: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var googleAuthClientId: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var googleAuthClientSecret: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var googleAuthDesktopClientId: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var googleAuthDesktopClientSecret: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    lateinit var assetLinksJson: String
    var samayeApiKey: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var twilioAccountSid: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var twilioAuthToken: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var twilioFromNumber: String? = null
        get() = if (!field.isNullOrBlank()) field else null
    var twilioVerifyServiceSid: String? = null
        get() = if (!field.isNullOrBlank()) field else null

    fun rpId(): String = URL(selfBaseUrl).host

    fun assetLinks(): List<AssetLink> {
        return Json.decodeFromString<List<AssetLink>>(assetLinksJson)
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