import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Properties
import org.gradle.plugins.signing.Sign

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
}
buildscript {
    configurations.all {
        resolutionStrategy {
            force("org.apache.commons:commons-compress:1.26.2")
        }
    }
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

group = (findProperty("group") as String)
version = (findProperty("version") as String)

subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("com.vanniktech.maven.publish")) {
            configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
                publishToMavenCentral()
                signAllPublications()

                pom {
                    inceptionYear = "2025"
                    url = "https://github.com/Vardan-Soft-Pvt-Ltd/AuthKMP"
                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                            distribution = "repo"
                        }
                    }
                    developers {
                        developer {
                            id = "pitampoudel"
                            name = "Pitam Poudel"
                            url = "https://www.pitam.com.np/"
                        }
                    }
                    scm {
                        url = "https://github.com/Vardan-Soft-Pvt-Ltd/AuthKMP"
                        connection = "scm:git:https://github.com/Vardan-Soft-Pvt-Ltd/AuthKMP.git"
                        developerConnection = "scm:git:ssh://git@github.com/Vardan-Soft-Pvt-Ltd/AuthKMP.git"
                    }
                }
            }

            // Disable signing for local publishing
            if (gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal") }) {
                tasks.withType<Sign>().configureEach {
                    enabled = false
                }
            }
        }
    }
}
