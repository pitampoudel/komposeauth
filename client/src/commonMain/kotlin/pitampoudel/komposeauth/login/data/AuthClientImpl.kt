package pitampoudel.komposeauth.login.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import pitampoudel.core.data.asResource
import pitampoudel.core.data.download
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.KmpFile
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.data.CountryResponse
import pitampoudel.komposeauth.core.data.Credential
import pitampoudel.komposeauth.core.data.LoginOptionsResponse
import pitampoudel.komposeauth.core.data.OAuth2Response
import pitampoudel.komposeauth.core.data.ProfileResponse
import pitampoudel.komposeauth.core.data.RegisterPublicKeyRequest
import pitampoudel.komposeauth.core.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.core.data.UpdateProfileRequest
import pitampoudel.komposeauth.core.data.VerifyPhoneOtpRequest
import pitampoudel.komposeauth.core.domain.ApiEndpoints.DEACTIVATE
import pitampoudel.komposeauth.core.domain.ApiEndpoints.KYC
import pitampoudel.komposeauth.core.domain.ApiEndpoints.KYC_ADDRESS
import pitampoudel.komposeauth.core.domain.ApiEndpoints.KYC_DOCUMENTS
import pitampoudel.komposeauth.core.domain.ApiEndpoints.KYC_PERSONAL_INFO
import pitampoudel.komposeauth.core.domain.ApiEndpoints.LOGIN
import pitampoudel.komposeauth.core.domain.ApiEndpoints.LOGIN_OPTIONS
import pitampoudel.komposeauth.core.domain.ApiEndpoints.LOGOUT
import pitampoudel.komposeauth.core.domain.ApiEndpoints.ME
import pitampoudel.komposeauth.core.domain.ApiEndpoints.UPDATE_PHONE_NUMBER
import pitampoudel.komposeauth.core.domain.ApiEndpoints.UPDATE_PROFILE
import pitampoudel.komposeauth.core.domain.ApiEndpoints.VERIFY_PHONE_NUMBER
import pitampoudel.komposeauth.login.domain.AuthClient
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.kyc.data.DocumentInformation
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.data.PersonalInformation
import pitampoudel.komposeauth.kyc.data.UpdateAddressDetailsRequest

internal class AuthClientImpl(val httpClient: HttpClient, val authUrl: String) : AuthClient {

    override suspend fun fetchLoginConfig(platform: Platform): Result<LoginOptionsResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$LOGIN_OPTIONS") {
                parameter("platform", platform)
            }.asResource { body<LoginOptionsResponse>() }
        }
    }

    override suspend fun login(
        credential: Credential
    ): Result<OAuth2Response> {
        return safeApiCall {
            httpClient.post("$authUrl/$LOGIN") {
                parameter("responseType", ResponseType.TOKEN)
                setBody(credential)
            }.asResource { body() }
        }
    }


    override suspend fun login(
        credential: Credential,
        responseType: ResponseType
    ): Result<HttpResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$LOGIN") {
                parameter("responseType", responseType)
                setBody(credential)
            }.asResource { this }
        }
    }


    override suspend fun fetchUserInfo(): Result<ProfileResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$ME").asResource { body() }
        }
    }

    override suspend fun deactivate(): Result<HttpResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$DEACTIVATE").asResource { this }
        }
    }

    override suspend fun verifyPhoneOtp(req: VerifyPhoneOtpRequest): Result<HttpResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$VERIFY_PHONE_NUMBER") {
                setBody(req)
            }.asResource { this }
        }
    }

    override suspend fun sendPhoneOtp(request: UpdatePhoneNumberRequest): Result<HttpResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$UPDATE_PHONE_NUMBER") {
                setBody(request)
            }.asResource { this }
        }
    }

    override suspend fun fetchMyKyc(): Result<KycResponse?> {
        return safeApiCall {
            val response = httpClient.get("$authUrl/$KYC")
            if (response.status.value == 404) {
                Result.Success(null)
            } else {
                response.asResource { body<KycResponse?>() }
            }
        }
    }

    override suspend fun submitKycPersonalInfo(body: PersonalInformation): Result<KycResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$KYC_PERSONAL_INFO") {
                setBody(body)
            }.asResource { body<KycResponse>() }
        }
    }

    override suspend fun submitKycDocuments(body: DocumentInformation): Result<KycResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$KYC_DOCUMENTS") {
                setBody(body)
            }.asResource { body<KycResponse>() }
        }
    }

    override suspend fun submitKycAddressDetails(body: UpdateAddressDetailsRequest): Result<KycResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$KYC_ADDRESS") {
                setBody(body)
            }.asResource { body<KycResponse>() }
        }
    }

    override suspend fun fetchCountries(): Result<List<CountryResponse>> {
        return safeApiCall {
            httpClient.get("$authUrl/countries.json").asResource { body() }
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<ProfileResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$UPDATE_PROFILE") { setBody(request) }.asResource { body() }
        }
    }

    override suspend fun fetchWebAuthnRegistrationOptions(): Result<String> {
        return safeApiCall {
            httpClient.post("$authUrl/webauthn/register/options").asResource { body() }
        }
    }

    override suspend fun registerPublicKey(request: RegisterPublicKeyRequest): Result<HttpResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/webauthn/register") {
                setBody(request)
            }.asResource { this }
        }
    }

    override suspend fun logout(): Result<HttpResponse> {
        val result = safeApiCall {
            httpClient.get("$authUrl/$LOGOUT") {
            }.asResource { this }
        }
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
        return result
    }

    override suspend fun download(url: String): Result<KmpFile> {
        return httpClient.download(url)
    }
}
