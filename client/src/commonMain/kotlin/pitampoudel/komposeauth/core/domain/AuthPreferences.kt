package pitampoudel.komposeauth.core.domain

import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.data.OAuth2TokenData
import pitampoudel.komposeauth.data.ProfileResponse

internal interface AuthPreferences {
    val authenticatedUser: Flow<ProfileResponse?>
    suspend fun saveAuthenticatedUser(
        tokenData: OAuth2TokenData,
        userInfoResponse: ProfileResponse
    )
    suspend fun saveUserProfile(data: ProfileResponse)
    suspend fun updateTokenData(tokenData: OAuth2TokenData)
    fun tokenData(): OAuth2TokenData?
    suspend fun clear()
}
