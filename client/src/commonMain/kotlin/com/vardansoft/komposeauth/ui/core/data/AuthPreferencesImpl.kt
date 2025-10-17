package com.vardansoft.komposeauth.ui.core.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.coroutines.toSuspendSettings
import com.vardansoft.komposeauth.data.OAuth2TokenData
import com.vardansoft.komposeauth.data.UserInfoResponse
import com.vardansoft.komposeauth.ui.core.domain.AuthPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSettingsApi::class)
internal class AuthPreferencesImpl(
    private val settings: ObservableSettings
) : AuthPreferences {
    private val suspendSettings = settings.toSuspendSettings()

    private object KEYS {
        const val OAUTH2_TOKEN_DATA = "oauth2_token_data"
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
        token: OAuth2TokenData,
        userInfoResponse: UserInfoResponse
    ) {
        settings.putString(KEYS.OAUTH2_TOKEN_DATA, Json.encodeToString(token))
        suspendSettings.putString(KEYS.USER_INFO, Json.encodeToString(userInfoResponse))
    }

    override suspend fun updateTokenData(token: OAuth2TokenData) {
        suspendSettings.putString(KEYS.OAUTH2_TOKEN_DATA, Json.encodeToString(token))
    }

    override suspend fun updateUserInformation(info: UserInfoResponse) {
        suspendSettings.putString(KEYS.USER_INFO, Json.encodeToString(info))
    }

    override fun oAuth2TokenData(): OAuth2TokenData? {
        return settings.getStringOrNull(KEYS.OAUTH2_TOKEN_DATA)?.let {
            try {
                Json.decodeFromString<OAuth2TokenData>(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun clear() {
        suspendSettings.remove(KEYS.OAUTH2_TOKEN_DATA)
        suspendSettings.remove(KEYS.USER_INFO)
    }

}
