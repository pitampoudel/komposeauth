package com.vardansoft.authx

import com.vardansoft.authx.data.Platform
import com.vardansoft.authx.data.Platform.DESKTOP
import com.vardansoft.authx.data.Platform.WEB
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

@Configuration
@ConfigurationProperties(prefix = "app")
class AppProperties {
    // TODO it is always localhost in development but it should be ip address
    var baseUrl: String? = null
        get() {
            val raw = field?.trim().orEmpty()
            if (raw.isNotEmpty()) return raw
            return "http://${getLocalIpAddress()}:8080"
        }
    lateinit var gcpBucketName: String
    lateinit var name: String
    lateinit var expectedGcpProjectId: String
    lateinit var logoUrl: String
    lateinit var googleAuthClientId: String
    lateinit var googleAuthClientSecret: String
    lateinit var googleAuthDesktopClientId: String
    lateinit var googleAuthDesktopClientSecret: String

    var samayeApiKey: String? = null
    var twilioAccountSid: String? = null
    var twilioAuthToken: String? = null
    var twilioFromNumber: String? = null
    var twilioVerifyServiceSid: String? = null

    fun googleClientId(platform: Platform): String {
        return when (platform) {
            DESKTOP -> googleAuthDesktopClientId
            WEB -> googleAuthClientId
        }
    }

    fun googleClientSecret(platform: Platform): String {
        return when (platform) {
            DESKTOP -> googleAuthDesktopClientSecret
            WEB -> googleAuthClientSecret
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