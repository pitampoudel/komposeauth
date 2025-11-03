package pitampoudel.komposeauth.core.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.coroutines.toSuspendSettings
import com.russhwolf.settings.observable.makeObservable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.data.OAuth2TokenData
import pitampoudel.komposeauth.data.ProfileResponse

@OptIn(ExperimentalSettingsApi::class)
internal class AuthPreferencesImpl private constructor() : AuthPreferences {
    private val settings: ObservableSettings by lazy { Settings().makeObservable() }
    private val suspendSettings by lazy { settings.toSuspendSettings() }

    private object KEYS {
        const val TOKEN_DATA = "token_data"
        const val USER_PROFILE = "user_profile"
    }

    override val authenticatedUser: Flow<ProfileResponse?> by lazy {
        settings.getStringOrNullFlow(KEYS.USER_PROFILE).map { stringValue ->
            stringValue?.let {
                try {
                    Json.decodeFromString<ProfileResponse>(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }.distinctUntilChanged()
    }


    override suspend fun saveAuthenticatedUser(
        tokenData: OAuth2TokenData,
        userInfoResponse: ProfileResponse
    ) {
        suspendSettings.putString(KEYS.TOKEN_DATA, Json.encodeToString(tokenData))
        suspendSettings.putString(KEYS.USER_PROFILE, Json.encodeToString(userInfoResponse))
    }

    override suspend fun saveUserProfile(data: ProfileResponse) {
        suspendSettings.putString(KEYS.USER_PROFILE, Json.encodeToString(data))
    }

    override suspend fun updateTokenData(tokenData: OAuth2TokenData) {
        suspendSettings.putString(KEYS.TOKEN_DATA, Json.encodeToString(tokenData))
    }

    override fun tokenData(): OAuth2TokenData? {
        return settings.getStringOrNull(KEYS.TOKEN_DATA)?.let {
            return try {
                Json.decodeFromString<OAuth2TokenData>(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null

            }
        }
    }

    override suspend fun clear() {
        suspendSettings.remove(KEYS.TOKEN_DATA)
        suspendSettings.remove(KEYS.USER_PROFILE)
    }

    companion object {
        private var INSTANCE: AuthPreferences? = null
        fun getInstance(): AuthPreferences {
            return INSTANCE ?: AuthPreferencesImpl().also {
                INSTANCE = it
            }
        }
    }
}
