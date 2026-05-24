package pitampoudel.komposeauth.core.data

import io.ktor.client.*
import io.ktor.client.engine.js.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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

@OptIn(ExperimentalWasmJsInterop::class)
fun HttpClientConfig<JsClientEngineConfig>.installKomposeAuth(
    authServerUrl: Flow<String?>,
    resourceServerUrls: List<String>,
    scope: CoroutineScope
) {
    val authPreferences = AuthPreferencesImpl.getInstance()
    engine {
        configureRequest {
            credentials = "include".toJsString()
        }
    }
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls, scope)
}