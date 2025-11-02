package pitampoudel.komposeauth.core.domain

import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.data.UserInfoResponse

internal interface AuthPreferences {
    val userInfoResponse: Flow<UserInfoResponse?>
    suspend fun saveUserInformation(info: UserInfoResponse)
    suspend fun clear()
}
