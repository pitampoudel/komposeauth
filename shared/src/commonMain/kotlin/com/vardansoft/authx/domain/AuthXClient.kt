package com.vardansoft.authx.domain

import com.vardansoft.authx.data.ConfigResponse
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.data.DocumentInformation
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.data.OAuth2TokenData
import com.vardansoft.authx.data.PersonalInformation
import com.vardansoft.authx.data.UpdateAddressDetailsRequest
import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.data.UserInfoResponse
import com.vardansoft.authx.data.VerifyPhoneOtpRequest
import com.vardansoft.authx.data.Country
import com.vardansoft.authx.data.Platform
import com.vardansoft.authx.data.UpdateProfileRequest
import com.vardansoft.core.domain.Result
import io.ktor.client.statement.HttpResponse

interface AuthXClient {
    suspend fun fetchConfig(platform: Platform = Platform.WEB): Result<ConfigResponse>
    suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData>
    suspend fun fetchUserInfo(accessToken: String? = null): Result<UserInfoResponse>
    suspend fun deactivate(): Result<HttpResponse>
    suspend fun verifyPhoneOtp(req: VerifyPhoneOtpRequest): Result<HttpResponse>
    suspend fun sendPhoneOtp(request: UpdatePhoneNumberRequest): Result<HttpResponse>
    suspend fun fetchMyKyc(): Result<KycResponse?>
    suspend fun submitKycPersonalInfo(body: PersonalInformation): Result<KycResponse>
    suspend fun submitKycDocuments(body: DocumentInformation): Result<KycResponse>
    suspend fun submitKycAddressDetails(body: UpdateAddressDetailsRequest): Result<KycResponse>
    suspend fun fetchCountries(): Result<List<Country>>
    suspend fun updateProfile(request: UpdateProfileRequest): Result<HttpResponse>
}
