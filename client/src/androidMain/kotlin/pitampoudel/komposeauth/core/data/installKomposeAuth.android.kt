package pitampoudel.komposeauth.core.data

import android.content.Context
import io.ktor.client.HttpClientConfig

fun HttpClientConfig<*>.installKomposeAuth(
    context: Context,
    authServerUrl: String,
    resourceServerUrls: List<String>
) {
    val authPreferences = AuthPreferencesImpl.getInstance(SecureSettingsFactory().create(context))
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls)
}