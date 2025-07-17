package com.vardansoft.auth.domain

import com.vardansoft.auth.data.OAuth2TokenData
import com.vardansoft.auth.data.UserInfo
import com.vardansoft.auth.presentation.Credential

interface LoginClient {
    suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData>
    suspend fun fetchUserInfo(accessToken: String): Result<UserInfo>
}
