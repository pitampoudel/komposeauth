package pitampoudel.komposeauth.core.data

import android.content.Context
import io.ktor.client.HttpClientConfig
import pitampoudel.komposeauth.login.data.AuthPreferencesImpl

fun HttpClientConfig<*>.installKomposeAuth(
    context: Context,
    authServerUrl: String,
    resourceServerUrls: List<String>
) {
    val authPreferences = AuthPreferencesImpl.getInstance()
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls)
}