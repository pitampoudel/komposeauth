package pitampoudel.komposeauth.login.domain

import io.ktor.client.statement.HttpResponse
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.data.CountryResponse
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.core.data.LoginOptionsResponse
import pitampoudel.komposeauth.core.data.OAuth2Response
import pitampoudel.komposeauth.user.data.ProfileResponse
import pitampoudel.komposeauth.core.data.RegisterPublicKeyRequest
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.UpdateProfileRequest
import pitampoudel.komposeauth.user.data.VerifyOtpRequest
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.kyc.data.DocumentInformation
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.data.PersonalInformation
import pitampoudel.komposeauth.kyc.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.user.data.SendEmailOtpRequest
import pitampoudel.komposeauth.user.data.VerifyEmailOtpRequest


internal interface AuthClient {
    suspend fun fetchLoginConfig(platform: Platform = Platform.WEB): Result<LoginOptionsResponse>
    suspend fun login(credential: Credential): Result<OAuth2Response>
    suspend fun login(credential: Credential, responseType: ResponseType): Result<HttpResponse>
    suspend fun fetchUserInfo(): Result<ProfileResponse>
    suspend fun deactivate(): Result<HttpResponse>
    suspend fun verifyPhoneOtp(req: VerifyOtpRequest): Result<HttpResponse>
    suspend fun sendPhoneOtp(request: SendOtpRequest): Result<HttpResponse>
    suspend fun fetchMyKyc(): Result<KycResponse?>
    suspend fun submitKycPersonalInfo(body: PersonalInformation): Result<KycResponse>
    suspend fun submitKycDocuments(body: DocumentInformation): Result<KycResponse>
    suspend fun submitKycAddressDetails(body: UpdateAddressDetailsRequest): Result<KycResponse>
    suspend fun fetchCountries(): Result<List<CountryResponse>>
    suspend fun updateProfile(request: UpdateProfileRequest): Result<ProfileResponse>
    suspend fun fetchWebAuthnRegistrationOptions(): Result<String>
    suspend fun registerPublicKey(request: RegisterPublicKeyRequest): Result<HttpResponse>
    suspend fun logout(): Result<HttpResponse>
    suspend fun sendEmailOtp(request: SendEmailOtpRequest): Result<HttpResponse>
    suspend fun verifyEmailOtp(req: VerifyEmailOtpRequest): Result<HttpResponse>
}