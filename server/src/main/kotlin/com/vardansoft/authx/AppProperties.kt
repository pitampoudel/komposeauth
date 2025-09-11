package com.vardansoft.authx

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.net.Inet4Address
import java.net.NetworkInterface

@Configuration
@ConfigurationProperties(prefix = "app")
class AppProperties {
    var baseUrl: String? = null
        get() {
            val raw = field?.trim().orEmpty()
            if (raw.isNotEmpty()) return raw
            val port = "8080"
            val ip = getLocalIpAddress() ?: "localhost"
            return "http://$ip:$port"
        }
    lateinit var gcpBucketName: String
    lateinit var name: String
    lateinit var expectedGcpProjectId: String
    lateinit var logoUrl: String

    var samayeApiKey: String? = null
    var twilioAccountSid: String? = null
    var twilioAuthToken: String? = null
    var twilioFromNumber: String? = null
    var twilioVerifyServiceSid: String? = null

    fun getLocalIpAddress(): String? {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces()
        while (networkInterfaces.hasMoreElements()) {
            val networkInterface = networkInterfaces.nextElement()
            val inetAddresses = networkInterface.inetAddresses
            while (inetAddresses.hasMoreElements()) {
                val inetAddress = inetAddresses.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return "${inetAddress.hostAddress}"
                }
            }
        }
        return null
    }
}