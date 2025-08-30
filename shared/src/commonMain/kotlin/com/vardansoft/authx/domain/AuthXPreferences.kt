package com.vardansoft.authx.domain

import com.vardansoft.authx.domain.LazyState
import com.vardansoft.authx.data.OAuth2TokenData
import com.vardansoft.authx.data.UserInfo
import kotlinx.coroutines.flow.Flow

interface AuthXPreferences {
    val userInfo: Flow<LazyState<UserInfo>>
    suspend fun saveLoggedInDetails(
        token: OAuth2TokenData,
        userInfo: UserInfo
    )

    suspend fun updateTokenData(token: OAuth2TokenData)
    suspend fun updateUserInformation(info: UserInfo)
    fun oAuth2TokenData(): OAuth2TokenData?
    suspend fun clear()

}
