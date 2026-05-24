package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.login.data.AuthPreferencesImpl

fun HttpClientConfig<*>.installKomposeAuth(
    authServerUrl: String,
    resourceServerUrls: List<String>
) {
    val authPreferences = AuthPreferencesImpl.getInstance()
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls)
}

fun HttpClientConfig<*>.installKomposeAuth(
    authServerUrl: Flow<String?>,
    resourceServerUrls: List<String>,
    scope: CoroutineScope
) {
    val authPreferences = AuthPreferencesImpl.getInstance()
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls, scope)
}