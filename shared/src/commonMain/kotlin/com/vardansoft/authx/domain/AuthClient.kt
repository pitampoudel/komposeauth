package com.vardansoft.authx.domain

import com.vardansoft.authx.data.ConfigResponse
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.data.OAuth2TokenData
import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.data.UserInfo
import com.vardansoft.authx.data.VerifyPhoneOtpRequest
import io.ktor.client.statement.HttpResponse

interface AuthClient {
    suspend fun fetchConfig(): Result<ConfigResponse>
    suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData>
    suspend fun fetchUserInfo(accessToken: String? = null): Result<UserInfo>
    suspend fun verifyPhoneOtp(req: VerifyPhoneOtpRequest): Result<HttpResponse>
    suspend fun sendPhoneOtp(request: UpdatePhoneNumberRequest): Result<HttpResponse>
}
