import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}
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

val propsFile = file("public.properties")
val props = Properties()
if (propsFile.exists()) {
    propsFile.inputStream().use { props.load(it) }
}

props.forEach {
    extra[it.key.toString()] = it.value
}
if (!extra.has("AUTH_URL")) extra["AUTH_URL"] = "http://" + getLocalIpAddress() + ":8080"
