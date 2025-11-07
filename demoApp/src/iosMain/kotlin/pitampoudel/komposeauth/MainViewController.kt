package pitampoudel.komposeauth

import BuildKonfig
import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import pitampoudel.komposeauth.core.data.installKomposeAuth
import pitampoudel.komposeauth.core.di.initializeKomposeAuth
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val httpClient = HttpClient {
        installKomposeAuth(
            authServerUrl = BuildKonfig.AUTH_SERVER_URL,
            resourceServerUrls = listOf(
                "https://your-resource-server"
            )
        )
        install(Logging) {
            level = LogLevel.BODY
        }
    }
    initializeKomposeAuth(
        httpClient = httpClient
    )
    return ComposeUIViewController { App() }
}