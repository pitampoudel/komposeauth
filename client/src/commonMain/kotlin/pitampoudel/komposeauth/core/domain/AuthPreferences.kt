package pitampoudel.komposeauth.core.domain

import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.data.UserInfoResponse

internal interface AuthPreferences {
    val authenticatedUserInfo: Flow<UserInfoResponse?>
    suspend fun saveAuthenticatedUserInfo(info: UserInfoResponse)
    suspend fun clear()
}
