package pitampoudel.komposeauth.core.data

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.observable.makeObservable
import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.login.data.AuthPreferencesImpl
import java.util.prefs.Preferences

/**
 * The tokens are kept in this app's node of the user's Java preferences: the registry under
 * `HKCU\Software\JavaSoft\Prefs` on Windows, `~/Library/Preferences/com.apple.java.util.prefs.plist`
 * on macOS and `~/.java/.userPrefs` on Linux. They are stored there **unencrypted**, readable by
 * anything running as the same OS user. Encrypting them would need a key kept where that user's
 * programs can't read it, and plain Java has no such place: the OS keystores (Keychain, Windows
 * Credential Manager or DPAPI, the Secret Service) are reachable only through native code, and a key
 * stored beside the tokens would hide nothing.
 *
 * @param appId names this app's storage, so that two apps using the SDK under one OS account don't
 * share, and spend, each other's sign-in. Use something unique to the app, such as its package name;
 * left out, it is the name of the main class or jar, which two apps may well share.
 */
fun HttpClientConfig<*>.installKomposeAuth(
    authServerUrl: String,
    resourceServerUrls: List<String>,
    appId: String = defaultAppId()
) {
    val authPreferences = AuthPreferencesImpl.getInstance(desktopSettings(appId))
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls)
}

/** @param appId as for the overload taking a fixed URL. */
fun HttpClientConfig<*>.installKomposeAuth(
    authServerUrl: Flow<String?>,
    resourceServerUrls: List<String>,
    scope: CoroutineScope,
    appId: String = defaultAppId()
) {
    val authPreferences = AuthPreferencesImpl.getInstance(desktopSettings(appId))
    installKomposeAuth(authPreferences, authServerUrl, resourceServerUrls, scope)
}

/** The app's own node of the user's Java preferences, under `komposeauth`. */
internal fun desktopSettings(appId: String): ObservableSettings {
    val name = appId.replace('/', '.').take(Preferences.MAX_NAME_LENGTH)
    require(name.isNotBlank()) { "appId must not be blank" }
    return PreferencesSettings(Preferences.userRoot().node("komposeauth").node(name)).makeObservable()
}

private fun defaultAppId(): String =
    System.getProperty("sun.java.command")
        ?.substringBefore(' ')
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.takeIf { it.isNotBlank() }
        ?: "app"
