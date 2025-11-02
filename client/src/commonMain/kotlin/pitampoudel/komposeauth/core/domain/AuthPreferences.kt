package pitampoudel.komposeauth.core.domain

import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.data.ProfileResponse

internal interface AuthPreferences {
    val authenticatedUser: Flow<ProfileResponse?>
    suspend fun saveAuthenticatedUser(profile: ProfileResponse)
    suspend fun clear()
}
