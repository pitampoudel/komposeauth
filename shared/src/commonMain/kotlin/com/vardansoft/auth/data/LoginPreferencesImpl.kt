package com.vardansoft.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.russhwolf.settings.Settings
import com.vardansoft.auth.domain.LoginPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class LoginPreferencesImpl(
    private val settings: Settings,
    private val dataStore: DataStore<Preferences>
) : LoginPreferences {

    private object KEYS {
        const val OAUTH2_TOKEN_DATA = "oauth2_token_data"
        val USER_INFO = stringPreferencesKey("user_info")
    }

    override fun oAuth2TokenData(): OAuth2TokenData? {
        return settings.getStringOrNull(KEYS.OAUTH2_TOKEN_DATA)?.let {
            try {
                Json.decodeFromString(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override val userInfo: Flow<UserInfo?> = dataStore.data.map {
        it[KEYS.USER_INFO]?.let { string ->
            try {
                Json.decodeFromString(string)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    }


    override suspend fun saveLoggedInDetails(
        token: OAuth2TokenData,
        userInfo: UserInfo
    ) {
        settings.putString(KEYS.OAUTH2_TOKEN_DATA, Json.encodeToString(token))
        dataStore.updateData {
            it.toMutablePreferences().apply {
                set(KEYS.USER_INFO, Json.encodeToString(userInfo))
            }
        }
    }

    override suspend fun updateTokenData(token: OAuth2TokenData) {
        settings.putString(KEYS.OAUTH2_TOKEN_DATA, Json.encodeToString(token))
    }

    override suspend fun updateUserInformation(info: UserInfo) {
        dataStore.updateData {
            it.toMutablePreferences().apply {
                set(KEYS.USER_INFO, Json.encodeToString(info))
            }
        }
    }

    override suspend fun clear() {
        settings.remove(KEYS.OAUTH2_TOKEN_DATA)
        dataStore.updateData {
            it.toMutablePreferences().apply {
                remove(KEYS.USER_INFO)
            }
        }
    }

}
