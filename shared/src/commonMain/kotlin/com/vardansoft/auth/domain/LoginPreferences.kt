package com.vardansoft.auth.domain

import com.vardansoft.auth.data.OAuth2TokenData
import com.vardansoft.auth.data.UserInfo
import kotlinx.coroutines.flow.Flow

interface LoginPreferences {
    val userInfo: Flow<UserInfo?>
    suspend fun saveLoggedInDetails(
        token: OAuth2TokenData,
        userInfo: UserInfo
    )

    suspend fun updateTokenData(token: OAuth2TokenData)
    suspend fun updateUserInformation(info: UserInfo)
    fun oAuth2TokenData(): OAuth2TokenData?
    suspend fun clear()

}
