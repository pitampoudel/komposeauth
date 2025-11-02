package pitampoudel.komposeauth.core.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.coroutines.toSuspendSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.data.ProfileResponse

@OptIn(ExperimentalSettingsApi::class)
internal class AuthPreferencesImpl(
    settings: ObservableSettings,
    val authStateChecker: AuthStateChecker
) : AuthPreferences {
    private val suspendSettings = settings.toSuspendSettings()

    private object KEYS {
        const val USER_INFO = "user_info"
    }

    override val authenticatedUser: Flow<ProfileResponse?> =
        settings.getStringOrNullFlow(KEYS.USER_INFO).map { stringValue ->
            if (authStateChecker.isLoggedIn() && stringValue != null)
                try {
                    Json.decodeFromString<ProfileResponse>(stringValue)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            else null
        }.distinctUntilChanged()


    override suspend fun saveAuthenticatedUser(profile: ProfileResponse) {
        suspendSettings.putString(KEYS.USER_INFO, Json.encodeToString(profile))
    }

    override suspend fun clear() {
        suspendSettings.remove(KEYS.USER_INFO)
    }

}
