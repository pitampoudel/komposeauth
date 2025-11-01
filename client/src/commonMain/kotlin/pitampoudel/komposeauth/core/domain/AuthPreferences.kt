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

    suspend fun updateTokenData(token: OAuth2TokenData)
    suspend fun updateUserInformation(info: UserInfoResponse)
    fun oAuth2TokenData(): OAuth2TokenData?
    suspend fun clear()

}
