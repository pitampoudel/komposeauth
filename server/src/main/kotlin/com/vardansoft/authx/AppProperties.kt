package com.vardansoft.authx

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.net.Inet4Address
import java.net.NetworkInterface

@Configuration
@ConfigurationProperties(prefix = "app")
class AppProperties {
    var baseUrl: String? = null
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

    init {
        // Ensure baseUrl has a sensible default as early as possible in the lifecycle
        if (baseUrl.isNullOrBlank()) {
            // We don't have access to server.port property directly here during binding, so default to 8080
            val port = "8080"
            // Avoid complex networking early; prefer localhost to ensure deterministic behavior
            val ip = getLocalIpAddress() ?: "localhost"
            val computed = "http://$ip:$port"
            baseUrl = computed
        }
    }
}