package com.vardansoft.komposeauth.ui.core.domain

import com.vardansoft.komposeauth.data.ConfigResponse
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.komposeauth.data.DocumentInformation
import com.vardansoft.komposeauth.data.KycResponse
import com.vardansoft.komposeauth.data.OAuth2TokenData
import com.vardansoft.komposeauth.data.PersonalInformation
import com.vardansoft.komposeauth.data.UpdateAddressDetailsRequest
import com.vardansoft.komposeauth.data.UpdatePhoneNumberRequest
import com.vardansoft.komposeauth.data.UserInfoResponse
import com.vardansoft.komposeauth.data.VerifyPhoneOtpRequest
import com.vardansoft.komposeauth.data.Country
import com.vardansoft.komposeauth.domain.Platform
import com.vardansoft.komposeauth.data.UpdateProfileRequest
import com.vardansoft.core.domain.Result
import io.ktor.client.statement.HttpResponse

internal interface AuthClient {
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
