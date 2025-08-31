package com.vardansoft.authx.domain

import com.vardansoft.authx.data.OAuth2TokenData
import com.vardansoft.authx.data.UserInfoResponse
import kotlinx.coroutines.flow.Flow

interface AuthXPreferences {
    val userInfoResponse: Flow<LazyState<UserInfoResponse>>
    suspend fun saveLoggedInDetails(
        token: OAuth2TokenData,
        userInfoResponse: UserInfoResponse
    )

    suspend fun updateTokenData(token: OAuth2TokenData)
    suspend fun updateUserInformation(info: UserInfoResponse)
    fun oAuth2TokenData(): OAuth2TokenData?
    suspend fun clear()

}
