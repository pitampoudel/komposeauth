package pitampoudel.komposeauth.domain

import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.data.StatsResponse
import pitampoudel.komposeauth.data.CreateUserRequest
import pitampoudel.komposeauth.data.OAuth2Response
import pitampoudel.komposeauth.data.UserResponse

interface SharedAuthClient {
    suspend fun fetchNewToken(scope: String): Result<OAuth2Response>
    suspend fun getOrCreateUser(accessToken: String, req: CreateUserRequest): Result<UserResponse>
    suspend fun fetchUsersInfo(
        userIds: List<String>,
        accessToken: String
    ): Result<Map<String, UserResponse>>

    suspend fun fetchUserInfo(userId: String, accessToken: String): Result<UserResponse>
    suspend fun getStats(accessToken: String): Result<StatsResponse>
}