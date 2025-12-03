package pitampoudel.komposeauth

import BuildKonfig
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.browser.document
import pitampoudel.komposeauth.core.data.installKomposeAuth
import pitampoudel.komposeauth.core.di.initializeKomposeAuth

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val httpClient = HttpClient(Js) {
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
    ComposeViewport(document.body!!) {
        App()
    }
}