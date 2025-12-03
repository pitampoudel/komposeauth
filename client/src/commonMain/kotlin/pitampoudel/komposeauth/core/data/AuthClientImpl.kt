package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
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
import pitampoudel.komposeauth.data.ApiEndpoints.LOGIN
import pitampoudel.komposeauth.data.ApiEndpoints.LOGIN_OPTIONS
import pitampoudel.komposeauth.data.ApiEndpoints.LOGOUT
import pitampoudel.komposeauth.data.ApiEndpoints.ME
import pitampoudel.komposeauth.data.ApiEndpoints.UPDATE_PHONE_NUMBER
import pitampoudel.komposeauth.data.ApiEndpoints.UPDATE_PROFILE
import pitampoudel.komposeauth.data.ApiEndpoints.VERIFY_PHONE_NUMBER
import pitampoudel.komposeauth.data.CountryResponse
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.DocumentInformation
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.data.LoginOptionsResponse
import pitampoudel.komposeauth.data.OAuth2Response
import pitampoudel.komposeauth.data.PersonalInformation
import pitampoudel.komposeauth.data.ProfileResponse
import pitampoudel.komposeauth.data.RegisterPublicKeyRequest
import pitampoudel.komposeauth.data.ResponseType
import pitampoudel.komposeauth.data.UpdateAddressDetailsRequest
import pitampoudel.komposeauth.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.data.UpdateProfileRequest
import pitampoudel.komposeauth.data.VerifyPhoneOtpRequest
import pitampoudel.komposeauth.domain.Platform

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


    override suspend fun fetchUserInfo(accessToken: String?): Result<ProfileResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$ME") {
                accessToken?.let {
                    bearerAuth(accessToken)
                }
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
        return safeApiCall {
            httpClient.get("$authUrl/$LOGOUT") {
            }.asResource { this }
        }
    }
}
