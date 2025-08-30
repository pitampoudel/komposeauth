package com.vardansoft.authx.data

import com.vardansoft.authx.data.AuthClientImpl.EndPoints.CONFIG
import com.vardansoft.authx.data.AuthClientImpl.EndPoints.TOKEN
import com.vardansoft.authx.data.AuthClientImpl.EndPoints.UPDATE_PHONE_NUMBER
import com.vardansoft.authx.data.AuthClientImpl.EndPoints.USER_INFO
import com.vardansoft.authx.data.AuthClientImpl.EndPoints.VERIFY_PHONE_NUMBER
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
    internal object EndPoints {
        const val TOKEN = "oauth2/token"
        const val USER_INFO = "userinfo"
        const val UPDATE_PHONE_NUMBER = "phone-number/update"
        const val VERIFY_PHONE_NUMBER = "phone-number/verify"
        const val CONFIG = "config"
    }

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
