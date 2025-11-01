package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import pitampoudel.core.data.asResource
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.data.ApiEndpoints.DEACTIVATE
import pitampoudel.komposeauth.data.ApiEndpoints.KYC
import pitampoudel.komposeauth.data.ApiEndpoints.KYC_ADDRESS
import pitampoudel.komposeauth.data.ApiEndpoints.KYC_DOCUMENTS
import pitampoudel.komposeauth.data.ApiEndpoints.KYC_PERSONAL_INFO
import pitampoudel.komposeauth.data.ApiEndpoints.LOGIN_OPTIONS
import pitampoudel.komposeauth.data.ApiEndpoints.ME
import pitampoudel.komposeauth.data.ApiEndpoints.TOKEN
import pitampoudel.komposeauth.data.ApiEndpoints.UPDATE_PHONE_NUMBER
import pitampoudel.komposeauth.data.ApiEndpoints.UPDATE_PROFILE
import pitampoudel.komposeauth.data.ApiEndpoints.VERIFY_PHONE_NUMBER
import pitampoudel.komposeauth.data.Country
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.DocumentInformation
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.data.LoginOptions
import pitampoudel.komposeauth.data.OAuth2TokenData
import pitampoudel.komposeauth.data.PersonalInformation
import pitampoudel.komposeauth.data.RegisterPublicKeyRequest
import pitampoudel.komposeauth.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.data.UpdateProfileRequest
import pitampoudel.komposeauth.data.UserInfoResponse
import pitampoudel.komposeauth.data.VerifyPhoneOtpRequest
import pitampoudel.komposeauth.domain.Platform

internal class AuthClientImpl(val httpClient: HttpClient, val authUrl: String) : AuthClient {

    override suspend fun fetchLoginConfig(platform: Platform): Result<LoginOptions> {
        return safeApiCall {
            httpClient.get("$authUrl/$LOGIN_OPTIONS") {
                parameter("platform", platform)
            }.asResource { body<LoginOptions>() }
        }
    }

    override suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData> {
        return safeApiCall {
            httpClient.post("$authUrl/$TOKEN") {
                setBody(credential)
            }.asResource { body() }
        }
    }


    override suspend fun fetchUserInfo(): Result<UserInfoResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$ME") {
            }.asResource { body() }
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

    override suspend fun fetchCountries(): Result<List<Country>> {
        return safeApiCall {
            httpClient.get("$authUrl/countries.json").asResource { body() }
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<HttpResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$UPDATE_PROFILE") { setBody(request) }.asResource { this }
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
}
