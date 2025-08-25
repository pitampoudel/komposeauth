package com.vardansoft.authx.data

import com.vardansoft.authx.EndPoints.TOKEN
import com.vardansoft.authx.EndPoints.UPDATE_PHONE_NUMBER
import com.vardansoft.authx.EndPoints.USER_INFO
import com.vardansoft.authx.EndPoints.VERIFY_PHONE_NUMBER
import com.vardansoft.authx.EndPoints.CONFIG
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.data.utils.asResource
import com.vardansoft.authx.data.utils.safeApiCall
import com.vardansoft.authx.domain.AuthClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters

class AuthClientImpl(val httpClient: HttpClient, val authUrl: String) : AuthClient {

    override suspend fun fetchConfig(): Result<ConfigResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/$CONFIG")
                .asResource { body<ConfigResponse>() }
        }
    }

    override suspend fun exchangeCredentialForToken(credential: Credential): Result<OAuth2TokenData> {
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


    override suspend fun fetchUserInfo(accessToken: String?): Result<UserInfo> {
        return safeApiCall {
            httpClient.get("$authUrl/$USER_INFO") {
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


}
