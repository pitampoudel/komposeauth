package com.vardansoft.auth.domain

import com.vardansoft.auth.com.vardansoft.auth.domain.Credential
import com.vardansoft.auth.data.OAuth2TokenData
import com.vardansoft.auth.data.UpdatePhoneNumberRequest
import com.vardansoft.auth.data.UserInfo
import com.vardansoft.auth.data.VerifyPhoneOtpRequest
import io.ktor.client.statement.HttpResponse

interface AuthClient {
    suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData>
    suspend fun fetchUserInfo(accessToken: String? = null): Result<UserInfo>
    suspend fun verifyPhoneOtp(req: VerifyPhoneOtpRequest): Result<HttpResponse>
    suspend fun sendPhoneOtp(request: UpdatePhoneNumberRequest): Result<HttpResponse>
}
