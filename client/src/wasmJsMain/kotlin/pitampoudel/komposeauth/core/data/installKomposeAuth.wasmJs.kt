package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.JsClientEngineConfig
import pitampoudel.komposeauth.login.data.AuthPreferencesImpl

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