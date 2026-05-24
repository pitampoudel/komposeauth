package pitampoudel.komposeauth.core.data

import android.content.Context
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.observable.makeObservable
import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.login.data.AuthPreferencesImpl

fun createSettings(context: Context): Settings {
    val sp = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sp)
}

fun HttpClientConfig<*>.installKomposeAuth(
    context: Context,
    authServerUrl: String,
    resourceServerUrls: List<String>
) {
    val authPreferences = AuthPreferencesImpl.getInstance(
        createSettings(context).makeObservable()
    )
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls)
}

fun HttpClientConfig<*>.installKomposeAuth(
    context: Context,
    authServerUrl: Flow<String?>,
    resourceServerUrls: List<String>,
    scope: CoroutineScope
) {
    val authPreferences = AuthPreferencesImpl.getInstance(
        createSettings(context).makeObservable()
    )
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls, scope)
}