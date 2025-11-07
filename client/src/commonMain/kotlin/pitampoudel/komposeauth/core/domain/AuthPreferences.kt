package pitampoudel.komposeauth.core.domain

import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.data.OAuth2Response
import pitampoudel.komposeauth.data.ProfileResponse

internal interface AuthPreferences {
    val authenticatedUser: Flow<ProfileResponse?>
    suspend fun saveAuthenticatedUser(
        tokenData: OAuth2Response,
        userInfoResponse: ProfileResponse
    )
    suspend fun saveUserProfile(data: ProfileResponse)
    suspend fun updateTokenData(tokenData: OAuth2Response)
    fun tokenData(): OAuth2Response?
    suspend fun clear()
}
