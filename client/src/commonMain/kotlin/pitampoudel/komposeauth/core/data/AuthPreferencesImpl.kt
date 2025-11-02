package pitampoudel.komposeauth.core.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.coroutines.toSuspendSettings
import pitampoudel.komposeauth.data.OAuth2TokenData
import pitampoudel.komposeauth.data.UserInfoResponse
import pitampoudel.komposeauth.core.domain.AuthPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSettingsApi::class)
internal class AuthPreferencesImpl(
    private val settings: ObservableSettings
) : AuthPreferences {
    private val suspendSettings = settings.toSuspendSettings()
    private var accessToken: String? = null

    private object KEYS {
        const val REFRESH_TOKEN = "refresh_token"
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


    override suspend fun saveLoggedInDetails(
        tokenData: OAuth2TokenData,
        userInfoResponse: UserInfoResponse
    ) {
        tokenData.refreshToken?.let {
            suspendSettings.putString(KEYS.REFRESH_TOKEN, it)
        } ?: run {
            suspendSettings.remove(KEYS.REFRESH_TOKEN)
        }
        suspendSettings.putString(KEYS.USER_INFO, Json.encodeToString(userInfoResponse))
        accessToken = tokenData.accessToken
    }

    override suspend fun updateTokenData(tokenData: OAuth2TokenData) {
        tokenData.refreshToken?.let {
            suspendSettings.putString(KEYS.REFRESH_TOKEN, it)
        } ?: run {
            suspendSettings.remove(KEYS.REFRESH_TOKEN)
        }
        accessToken = tokenData.accessToken
    }

    override suspend fun updateUserInformation(info: UserInfoResponse) {
        suspendSettings.putString(KEYS.USER_INFO, Json.encodeToString(info))
    }

    override fun accessToken() = accessToken
    override fun refreshToken() = settings.getStringOrNull(KEYS.REFRESH_TOKEN)

    override suspend fun clear() {
        accessToken = null
        suspendSettings.remove(KEYS.REFRESH_TOKEN)
        suspendSettings.remove(KEYS.USER_INFO)
    }

}
