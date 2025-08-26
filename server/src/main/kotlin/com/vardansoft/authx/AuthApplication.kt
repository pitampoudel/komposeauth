package com.vardansoft.authx

import com.vardansoft.authx.core.utils.GcpUtils
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.net.Inet4Address
import java.net.NetworkInterface

@SpringBootApplication
class AuthApplication {

    @Bean
    fun startupChecks(appProperties: AppProperties): ApplicationRunner = ApplicationRunner {
        GcpUtils.assertAuthenticatedProject(appProperties.expectedGcpProjectId)
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

        if (appProperties.baseUrl == null) {
            val port = "8080"
            val ip = getLocalIpAddress() ?: "localhost"
            val computed = "http://$ip:$port"
            appProperties.baseUrl = computed
        }
    }
}

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}