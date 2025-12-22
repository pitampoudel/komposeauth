package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClientConfig
import pitampoudel.komposeauth.login.data.AuthPreferencesImpl

fun HttpClientConfig<*>.installKomposeAuth(
    authServerUrl: String,
    resourceServerUrls: List<String>
) {
    val authPreferences = AuthPreferencesImpl.getInstance(SecureSettingsFactory().create())
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls)
}