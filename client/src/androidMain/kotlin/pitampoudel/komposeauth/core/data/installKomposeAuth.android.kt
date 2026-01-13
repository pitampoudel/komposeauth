package pitampoudel.komposeauth.core.data

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.observable.makeObservable
import io.ktor.client.HttpClientConfig
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