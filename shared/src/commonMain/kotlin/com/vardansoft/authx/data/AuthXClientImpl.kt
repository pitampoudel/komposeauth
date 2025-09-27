package com.vardansoft.authx.data

import com.vardansoft.authx.data.ApiEndpoints.CONFIG
import com.vardansoft.authx.data.ApiEndpoints.KYC
import com.vardansoft.authx.data.ApiEndpoints.ME
import com.vardansoft.authx.data.ApiEndpoints.TOKEN
import com.vardansoft.authx.data.ApiEndpoints.UPDATE_PHONE_NUMBER
import com.vardansoft.authx.data.ApiEndpoints.VERIFY_PHONE_NUMBER
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.core.data.asResource
import com.vardansoft.core.data.safeApiCall
import com.vardansoft.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

class AuthXClientImpl(val httpClient: HttpClient, val authUrl: String) : AuthXClient {

    override suspend fun fetchConfig(desktop: Boolean): Result<ConfigResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$CONFIG") {
                parameter("desktop", desktop.toString())
            }.asResource { body<ConfigResponse>() }
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
}
