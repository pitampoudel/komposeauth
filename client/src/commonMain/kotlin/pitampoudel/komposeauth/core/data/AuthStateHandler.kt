package pitampoudel.komposeauth.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pitampoudel.core.domain.Result
import pitampoudel.core.presentation.LazyState
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.core.domain.AuthUser

internal class AuthStateHandler(
    private val authPreferences: AuthPreferences,
    private val authClient: AuthClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentUser = MutableStateFlow<LazyState<AuthUser>>(LazyState.Loading)
    val currentUser: StateFlow<LazyState<AuthUser>> = _currentUser.asStateFlow()

    init {
        authPreferences.accessTokenPayload.onEach { string ->
            val user = string?.let {
                try {
                    json.decodeFromString<AuthUser>(string)
                } catch (ex: SerializationException) {
                    authPreferences.clear()
                    null
                }
            }
            updateCurrentUser(user)
        }.launchIn(scope)
    }


    suspend fun updateCurrentUser(fallbackUser: AuthUser? = null) {
        if (fallbackUser != null) _currentUser.value = LazyState.Loaded(fallbackUser)
        val fetchResult = fetchUserInfo()
        _currentUser.value = LazyState.Loaded(
            when (fetchResult) {
                is Result.Error -> fallbackUser
                is Result.Success<AuthUser> -> fetchResult.data
            }
        )
    }

    suspend fun fetchUserInfo(): Result<AuthUser> {
        return when (val res = authClient.fetchUserInfo()) {
            is Result.Success -> Result.Success(
                AuthUser(
                    authorities = res.data.roles,
                    email = res.data.email,
                    familyName = res.data.familyName,
                    givenName = res.data.givenName,
                    kycVerified = res.data.kycVerified,
                    phoneNumberVerified = res.data.phoneNumberVerified,
                    picture = res.data.picture ?: "",
                    sub = res.data.id
                )
            )

            is Result.Error -> res
        }
    }

    suspend fun logout() {
        val res = authClient.logout()
        if (res is Result.Success) authPreferences.clear()
    }
}

