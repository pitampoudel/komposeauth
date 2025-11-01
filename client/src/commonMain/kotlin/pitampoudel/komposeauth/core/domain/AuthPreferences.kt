package pitampoudel.komposeauth.core.domain

import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.data.OAuth2TokenData
import pitampoudel.komposeauth.data.UserInfoResponse

internal interface AuthPreferences {
    val userInfoResponse: Flow<UserInfoResponse?>
    suspend fun saveLoggedInDetails(
        tokenData: OAuth2TokenData,
        userInfoResponse: UserInfoResponse
    )
    suspend fun updateTokenData(tokenData: OAuth2TokenData)
    suspend fun updateUserInformation(info: UserInfoResponse)
    fun accessToken(): String?
    fun refreshToken(): String?
    suspend fun clear()
}
