package com.vardansoft.komposeauth.core.data

import com.vardansoft.core.data.MessageResponse
import com.vardansoft.core.data.asResource
import com.vardansoft.core.data.safeApiCall
import com.vardansoft.core.domain.Result
import com.vardansoft.komposeauth.core.domain.AuthClient
import com.vardansoft.komposeauth.data.ApiEndpoints.CONFIG
import com.vardansoft.komposeauth.data.ApiEndpoints.DEACTIVATE
import com.vardansoft.komposeauth.data.ApiEndpoints.KYC
import com.vardansoft.komposeauth.data.ApiEndpoints.ME
import com.vardansoft.komposeauth.data.ApiEndpoints.TOKEN
import com.vardansoft.komposeauth.data.ApiEndpoints.UPDATE
import com.vardansoft.komposeauth.data.ApiEndpoints.UPDATE_PHONE_NUMBER
import com.vardansoft.komposeauth.data.ApiEndpoints.VERIFY_PHONE_NUMBER
import com.vardansoft.komposeauth.data.Country
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.komposeauth.data.DocumentInformation
import com.vardansoft.komposeauth.data.KycResponse
import com.vardansoft.komposeauth.data.LoginConfigResponse
import com.vardansoft.komposeauth.data.OAuth2TokenData
import com.vardansoft.komposeauth.data.PersonalInformation
import com.vardansoft.komposeauth.data.UpdateAddressDetailsRequest
import com.vardansoft.komposeauth.data.UpdatePhoneNumberRequest
import com.vardansoft.komposeauth.data.UpdateProfileRequest
import com.vardansoft.komposeauth.data.UserInfoResponse
import com.vardansoft.komposeauth.data.VerifyPhoneOtpRequest
import com.vardansoft.komposeauth.domain.Platform
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

internal class AuthClientImpl(val httpClient: HttpClient, val authUrl: String) : AuthClient {

    override suspend fun fetchLoginConfig(platform: Platform): Result<LoginConfigResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$CONFIG/login") {
                parameter("platform", platform)
            }.asResource { body<LoginConfigResponse>() }
        }
    }

    override suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData> {
        return safeApiCall {
            httpClient.post("$authUrl/$TOKEN") {
                setBody(credential)
            }.asResource { body() }
        }
    }


    override suspend fun fetchUserInfo(accessToken: String?): Result<UserInfoResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$ME") {
                accessToken?.let { bearerAuth(accessToken) }
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
            httpClient.post("$authUrl/$KYC/personal-info") {
                setBody(body)
            }.asResource { body<KycResponse>() }
        }
    }

    override suspend fun submitKycDocuments(body: DocumentInformation): Result<KycResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$KYC/documents") {
                setBody(body)
            }.asResource { body<KycResponse>() }
        }
    }

    override suspend fun submitKycAddressDetails(body: UpdateAddressDetailsRequest): Result<KycResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$KYC/address") {
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
            httpClient.post("$authUrl/$UPDATE") { setBody(request) }.asResource { this }
        }
    }

    override suspend fun fetchRegistrationOptions(): Result<String> {
        return safeApiCall {
            httpClient.post("$authUrl/webauthn/register/options").asResource { body() }
        }
    }

    override suspend fun registerPublicKey(request: String): Result<MessageResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/webauthn/register") {
                setBody(request)
            }.asResource { body() }
        }
    }
}
