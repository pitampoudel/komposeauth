package com.vardansoft.auth.data

import com.vardansoft.auth.EndPoints.TOKEN
import com.vardansoft.auth.EndPoints.UPDATE_PHONE_NUMBER
import com.vardansoft.auth.EndPoints.USER_INFO
import com.vardansoft.auth.EndPoints.VERIFY_PHONE_NUMBER
import com.vardansoft.auth.EndPoints.apiUrl
import com.vardansoft.auth.com.vardansoft.auth.domain.Credential
import com.vardansoft.auth.data.utils.asResource
import com.vardansoft.auth.data.utils.safeApiCall
import com.vardansoft.auth.domain.AuthClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters

class AuthClientImpl(val httpClient: HttpClient) : AuthClient {

    override suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData> {
        return safeApiCall {
            httpClient.submitForm(
                apiUrl(TOKEN),
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


    override suspend fun fetchUserInfo(accessToken: String?): Result<UserInfo> {
        return safeApiCall {
            httpClient.get(apiUrl(USER_INFO)) {
                accessToken?.let { bearerAuth(accessToken) }
            }.asResource { body() }
        }
    }

    override suspend fun verifyPhoneOtp(req: VerifyPhoneOtpRequest): Result<HttpResponse> {
        return safeApiCall {
            httpClient.post(apiUrl(VERIFY_PHONE_NUMBER)) {
                setBody(req)
            }.asResource { this }
        }
    }

    override suspend fun sendPhoneOtp(request: UpdatePhoneNumberRequest): Result<HttpResponse> {
        return safeApiCall {
            httpClient.post(apiUrl(UPDATE_PHONE_NUMBER)) {
                setBody(request)
            }.asResource { this }
        }
    }


}
