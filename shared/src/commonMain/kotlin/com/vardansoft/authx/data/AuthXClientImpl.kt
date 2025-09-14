package com.vardansoft.authx.data

import com.vardansoft.authx.data.ApiEndpoints.CONFIG
import com.vardansoft.authx.data.ApiEndpoints.KYC
import com.vardansoft.authx.data.ApiEndpoints.TOKEN
import com.vardansoft.authx.data.ApiEndpoints.UPDATE_PHONE_NUMBER
import com.vardansoft.authx.data.ApiEndpoints.USER_INFO
import com.vardansoft.authx.data.ApiEndpoints.VERIFY_PHONE_NUMBER
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.core.data.NetworkResult
import com.vardansoft.core.data.asResource
import com.vardansoft.core.data.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters

class AuthXClientImpl(val httpClient: HttpClient, val authUrl: String) : AuthXClient {

    override suspend fun fetchConfig(): NetworkResult<ConfigResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$CONFIG")
                .asResource { body<ConfigResponse>() }
        }
    }

    override suspend fun exchangeCredentialForToken(credential: Credential): NetworkResult<OAuth2TokenData> {
        return safeApiCall {
            httpClient.submitForm(
                "$authUrl/$TOKEN",
                formParameters = when (credential) {
                    is Credential.GoogleId -> {
                        Parameters.build {
                            append("code", credential.idToken)
                            append("client_id", credential.clientId)
                            append("grant_type", "urn:ietf:params:oauth:grant-type:google_id_token")
                        }
                    }
                }
            ).asResource { body() }
        }
    }


    override suspend fun fetchUserInfo(accessToken: String?): NetworkResult<UserInfoResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$USER_INFO") {
                accessToken?.let { bearerAuth(accessToken) }
            }.asResource { body() }
        }
    }

    override suspend fun verifyPhoneOtp(req: VerifyPhoneOtpRequest): NetworkResult<HttpResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$VERIFY_PHONE_NUMBER") {
                setBody(req)
            }.asResource { this }
        }
    }

    override suspend fun sendPhoneOtp(request: UpdatePhoneNumberRequest): NetworkResult<HttpResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$UPDATE_PHONE_NUMBER") {
                setBody(request)
            }.asResource { this }
        }
    }

    override suspend fun fetchMyKyc(): NetworkResult<KycResponse?> {
        return safeApiCall {
            val response = httpClient.get("$authUrl/$KYC")
            if (response.status.value == 404) {
                NetworkResult.Success(null)
            } else {
                response.asResource { body<KycResponse?>() }
            }
        }
    }

    override suspend fun submitKyc(body: UpdateKycRequest): NetworkResult<KycResponse> {
        return safeApiCall {
            httpClient.post("$authUrl/$KYC") {
                setBody(body)
            }.asResource { body<KycResponse>() }
        }
    }

}
