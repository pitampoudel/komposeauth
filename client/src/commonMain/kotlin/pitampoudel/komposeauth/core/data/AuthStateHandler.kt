package pitampoudel.komposeauth.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    init {
        scope.launch {
            authPreferences.accessTokenPayload.collect { string ->
                val user = string?.let {
                    try {
                        json.decodeFromString<AuthUser>(string)
                    } catch (ex: SerializationException) {
                        authPreferences.clear()
                        null
                    }
                } ?: fetchUserInfo()
                _currentUser.value = user
            }
        }
    }

    fun refreshCurrentUser() {
        scope.launch {
            _currentUser.value = fetchUserInfo()
        }
    }

    suspend fun fetchUserInfo(): AuthUser? {
        return when (val res = authClient.fetchUserInfo()) {
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
        val res = authClient.logout()
        if (res is Result.Success) refreshCurrentUser()
    }
}

