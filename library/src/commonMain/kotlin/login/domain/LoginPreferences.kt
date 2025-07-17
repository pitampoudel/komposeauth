package com.vardansoft.auth.login.domain

import com.vardansoft.auth.login.data.OAuth2TokenData
import com.vardansoft.auth.login.data.UserInfo
import kotlinx.coroutines.flow.Flow

interface LoginPreferences {
    val userInfo: Flow<UserInfo?>
    suspend fun saveLoggedInDetails(
        token: OAuth2TokenData,
        userInfo: UserInfo
    )

    suspend fun updateTokenData(token: OAuth2TokenData)
    fun oAuth2TokenData(): OAuth2TokenData?
    suspend fun clear()
}
