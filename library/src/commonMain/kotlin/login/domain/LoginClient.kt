package com.vardansoft.auth.login.domain

import com.vardansoft.auth.login.data.OAuth2TokenData
import com.vardansoft.auth.login.data.UserInfo
import com.vardansoft.auth.login.presentation.Credential

interface LoginClient {
    suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData>
    suspend fun fetchUserInfo(accessToken: String): Result<UserInfo>
}
