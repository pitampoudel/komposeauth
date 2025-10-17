package com.vardansoft.komposeauth.ui.core.domain

import com.vardansoft.komposeauth.data.OAuth2TokenData
import com.vardansoft.komposeauth.data.UserInfoResponse
import kotlinx.coroutines.flow.Flow

internal interface AuthPreferences {
    val userInfoResponse: Flow<UserInfoResponse?>
    suspend fun saveLoggedInDetails(
        token: OAuth2TokenData,
        userInfoResponse: UserInfoResponse
    )

    suspend fun updateTokenData(token: OAuth2TokenData)
    suspend fun updateUserInformation(info: UserInfoResponse)
    fun oAuth2TokenData(): OAuth2TokenData?
    suspend fun clear()

}
