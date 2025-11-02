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
import pitampoudel.komposeauth.data.UserInfoResponse

@OptIn(ExperimentalSettingsApi::class)
internal class AuthPreferencesImpl(
    private val settings: ObservableSettings
) : AuthPreferences {
    private val suspendSettings = settings.toSuspendSettings()

    private object KEYS {
        const val USER_INFO = "user_info"
    }

    override val userInfoResponse: Flow<UserInfoResponse?> =
        settings.getStringOrNullFlow(KEYS.USER_INFO).map { stringValue ->
            stringValue?.let {
                try {
                    Json.decodeFromString<UserInfoResponse>(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }.distinctUntilChanged()


    override suspend fun saveUserInformation(info: UserInfoResponse) {
        suspendSettings.putString(KEYS.USER_INFO, Json.encodeToString(info))
    }

    override suspend fun clear() {
        suspendSettings.remove(KEYS.USER_INFO)
    }

}
