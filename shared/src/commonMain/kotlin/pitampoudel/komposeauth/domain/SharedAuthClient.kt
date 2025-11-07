package pitampoudel.komposeauth.domain

import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.data.CreateUserRequest
import pitampoudel.komposeauth.data.OAuth2Response
import pitampoudel.komposeauth.data.UserResponse

interface SharedAuthClient {

    suspend fun getOrCreateUser(accessToken: String, req: CreateUserRequest): Result<UserResponse>
    suspend fun fetchUsersInfo(
        userIds: List<String>,
        accessToken: String
    ): Result<Map<String, UserResponse>>

    suspend fun fetchUserInfo(userId: String, accessToken: String): Result<UserResponse>
    suspend fun fetchNewToken(scope: String): Result<OAuth2Response>
}