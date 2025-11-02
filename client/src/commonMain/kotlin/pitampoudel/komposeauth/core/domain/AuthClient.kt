package pitampoudel.komposeauth.core.domain

import io.ktor.client.statement.HttpResponse
import pitampoudel.komposeauth.data.Country
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.DocumentInformation
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.data.LoginOptions
import pitampoudel.komposeauth.data.PersonalInformation
import pitampoudel.komposeauth.data.RegisterPublicKeyRequest
import pitampoudel.komposeauth.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.data.UpdateProfileRequest
import pitampoudel.komposeauth.data.ProfileResponse
import pitampoudel.komposeauth.data.VerifyPhoneOtpRequest
import pitampoudel.komposeauth.domain.Platform
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.data.OAuth2TokenData


internal interface AuthClient {
    suspend fun fetchLoginConfig(platform: Platform = Platform.WEB): Result<LoginOptions>
    suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData>
    suspend fun fetchUserInfo(): Result<ProfileResponse>
    suspend fun deactivate(): Result<HttpResponse>
    suspend fun verifyPhoneOtp(req: VerifyPhoneOtpRequest): Result<HttpResponse>
    suspend fun sendPhoneOtp(request: UpdatePhoneNumberRequest): Result<HttpResponse>
    suspend fun fetchMyKyc(): Result<KycResponse?>
    suspend fun submitKycPersonalInfo(body: PersonalInformation): Result<KycResponse>
    suspend fun submitKycDocuments(body: DocumentInformation): Result<KycResponse>
    suspend fun submitKycAddressDetails(body: UpdateAddressDetailsRequest): Result<KycResponse>
    suspend fun fetchCountries(): Result<List<Country>>
    suspend fun updateProfile(request: UpdateProfileRequest): Result<HttpResponse>
    suspend fun fetchWebAuthnRegistrationOptions(): Result<String>
    suspend fun registerPublicKey(request: RegisterPublicKeyRequest): Result<HttpResponse>
}