package pitampoudel.komposeauth.core.data

import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.core.domain.AuthUser

internal class AuthStateHandler(
    private val authPreferences: AuthPreferences,
    private val authClient: AuthClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    val currentUser: Flow<AuthUser?> = authPreferences.accessTokenPayload.map { string ->
        string?.let {
            try {
                json.decodeFromString<AuthUser>(string)
            } catch (ex: SerializationException) {
                authPreferences.clear()
                null
            }
        } ?: when (val res = authClient.fetchUserInfo()) {
            is Result.Success -> AuthUser(
                authorities = res.data.roles,
                email = res.data.email,
                familyName = res.data.familyName,
                givenName = res.data.givenName,
                kycVerified = res.data.kycVerified,
                phoneNumberVerified = res.data.phoneNumberVerified,
                picture = res.data.picture ?: "",
                sub = res.data.id
            )
            is Result.Error -> null
        }
    }

    suspend fun logout() {
        authPreferences.clear()
        // todo also call backend logout to clear cookies
    }
}

