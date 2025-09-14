package com.vardansoft.authx.domain

import com.vardansoft.authx.data.ConfigResponse
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.data.OAuth2TokenData
import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.data.UserInfoResponse
import com.vardansoft.authx.data.VerifyPhoneOtpRequest
import com.vardansoft.authx.data.UpdateKycRequest
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.core.data.NetworkResult
import io.ktor.client.statement.HttpResponse

interface AuthXClient {
    suspend fun fetchConfig(): NetworkResult<ConfigResponse>
    suspend fun exchangeCredentialForToken(credential: Credential): NetworkResult<OAuth2TokenData>
    suspend fun fetchUserInfo(accessToken: String? = null): NetworkResult<UserInfoResponse>
    suspend fun verifyPhoneOtp(req: VerifyPhoneOtpRequest): NetworkResult<HttpResponse>
    suspend fun sendPhoneOtp(request: UpdatePhoneNumberRequest): NetworkResult<HttpResponse>
    suspend fun fetchMyKyc(): NetworkResult<KycResponse?>
    suspend fun submitKyc(body: UpdateKycRequest): NetworkResult<KycResponse>
}
