package com.vardansoft.auth.data

import com.vardansoft.auth.VardanSoftAuth.EndPoints.TOKEN
import com.vardansoft.auth.VardanSoftAuth.EndPoints.USER_INFO
import com.vardansoft.auth.VardanSoftAuth.EndPoints.apiUrl
import com.vardansoft.auth.data.utils.asResource
import com.vardansoft.auth.data.utils.safeApiCall
import com.vardansoft.auth.domain.LoginClient
import com.vardansoft.auth.presentation.Credential
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.Parameters

class LoginClientImpl(val httpClient: HttpClient) : LoginClient {

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


    override suspend fun fetchUserInfo(accessToken: String): Result<UserInfo> {
        return safeApiCall {
            httpClient.get(apiUrl(USER_INFO)) {
                bearerAuth(accessToken)
            }.asResource { body() }
        }
    }


}
