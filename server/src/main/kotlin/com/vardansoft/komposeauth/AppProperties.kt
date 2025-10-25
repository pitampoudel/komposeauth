package com.vardansoft.komposeauth

import com.vardansoft.komposeauth.domain.Platform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.net.Inet4Address
import java.net.NetworkInterface
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
    var baseUrl: String? = null
        get() {
            val raw = field?.trim().orEmpty()
            if (raw.isNotEmpty()) return raw
            return "http://${getLocalIpAddress()}:8080"
        }

    fun baseUrl() = baseUrl!!
    lateinit var gcpBucketName: String
    lateinit var name: String
    lateinit var expectedGcpProjectId: String
    lateinit var logoUrl: String
    lateinit var googleAuthClientId: String
    lateinit var googleAuthClientSecret: String
    lateinit var googleAuthDesktopClientId: String
    lateinit var googleAuthDesktopClientSecret: String
    lateinit var assetLinksJson: String

    fun assetLinks(): List<AssetLink> {
        return Json.decodeFromString<List<AssetLink>>(assetLinksJson)
    }

    var samayeApiKey: String? = null
    var twilioAccountSid: String? = null
    var twilioAuthToken: String? = null
    var twilioFromNumber: String? = null
    var twilioVerifyServiceSid: String? = null

    fun googleClientId(platform: Platform): String {
        return when (platform) {
            Platform.DESKTOP -> googleAuthDesktopClientId
            Platform.WEB -> googleAuthClientId
            Platform.ANDROID -> googleAuthClientId
            Platform.IOS -> googleAuthClientId
        }
    }

    fun googleClientSecret(platform: Platform): String {
        return when (platform) {
            Platform.DESKTOP -> googleAuthDesktopClientSecret
            Platform.WEB -> googleAuthClientSecret
            Platform.ANDROID -> googleAuthClientSecret
            Platform.IOS -> googleAuthClientSecret
        }
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