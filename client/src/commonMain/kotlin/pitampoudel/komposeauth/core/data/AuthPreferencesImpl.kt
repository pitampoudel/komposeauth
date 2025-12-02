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
import pitampoudel.komposeauth.core.data.JwtUtils.decodeAndParseJwtPayload
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.data.OAuth2Response

@OptIn(ExperimentalSettingsApi::class)
internal class AuthPreferencesImpl internal constructor(
    val settings: ObservableSettings
) : AuthPreferences {
    private val suspendSettings by lazy { settings.toSuspendSettings() }

    private object KEYS {
        const val TOKEN_DATA = "token_data"
    }

    override val accessTokenPayload: Flow<String?> by lazy {
        settings.getStringOrNullFlow(KEYS.TOKEN_DATA).map { tokenString ->
            tokenString?.let {
                try {
                    val tokenData = Json.decodeFromString<OAuth2Response>(it)
                    decodeAndParseJwtPayload(tokenData.accessToken)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }.distinctUntilChanged()
    }


    override suspend fun saveTokenData(tokenData: OAuth2Response) {
        suspendSettings.putString(KEYS.TOKEN_DATA, Json.encodeToString(tokenData))
    }

    override fun tokenData(): OAuth2Response? {
        return settings.getStringOrNull(KEYS.TOKEN_DATA)?.let {
            return try {
                Json.decodeFromString<OAuth2Response>(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null

            }
        }
    }

    override suspend fun clear() {
        suspendSettings.remove(KEYS.TOKEN_DATA)
    }

    companion object {
        private var INSTANCE: AuthPreferences? = null
        fun getInstance(settings: ObservableSettings = Settings().makeObservable()): AuthPreferences {
            return INSTANCE ?: AuthPreferencesImpl(settings).also {
                INSTANCE = it
            }
        }
    }
}
