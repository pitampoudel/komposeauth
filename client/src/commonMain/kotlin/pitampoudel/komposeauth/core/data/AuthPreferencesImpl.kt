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
    private var accessToken: String? = null

    private object KEYS {
        const val REFRESH_TOKEN = "refresh_token"
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
        tokenData.refreshToken?.let {
            suspendSettings.putString(KEYS.REFRESH_TOKEN, it)
        } ?: run {
            suspendSettings.remove(KEYS.REFRESH_TOKEN)
        }
        accessToken = tokenData.accessToken
        suspendSettings.putString(KEYS.USER_PROFILE, Json.encodeToString(userInfoResponse))
    }

    override suspend fun saveUserProfile(data: ProfileResponse) {
        suspendSettings.putString(KEYS.USER_PROFILE, Json.encodeToString(data))
    }

    override suspend fun updateTokenData(tokenData: OAuth2TokenData) {
        tokenData.refreshToken?.let {
            suspendSettings.putString(KEYS.REFRESH_TOKEN, it)
        } ?: run {
            suspendSettings.remove(KEYS.REFRESH_TOKEN)
        }
        accessToken = tokenData.accessToken
    }

    override suspend fun updateUserProfile(info: ProfileResponse) {
        suspendSettings.putString(KEYS.USER_PROFILE, Json.encodeToString(info))
    }

    override fun accessToken() = accessToken
    override fun refreshToken() = settings.getStringOrNull(KEYS.REFRESH_TOKEN)

    override suspend fun clear() {
        accessToken = null
        suspendSettings.remove(KEYS.REFRESH_TOKEN)
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
