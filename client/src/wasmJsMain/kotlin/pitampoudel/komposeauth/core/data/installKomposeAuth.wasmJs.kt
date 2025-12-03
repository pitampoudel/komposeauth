package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.JsClientEngineConfig

@OptIn(ExperimentalWasmJsInterop::class)
fun HttpClientConfig<JsClientEngineConfig>.installKomposeAuth(
    authServerUrl: String,
    resourceServerUrls: List<String>
) {
    val authPreferences = AuthPreferencesImpl.getInstance()
    engine {
        configureRequest {
            credentials = "include".toJsString()
        }
    }
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls)
}