package com.vardansoft.komposeauth.data


import com.vardansoft.core.data.asResource
import com.vardansoft.core.data.safeApiCall
import com.vardansoft.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters

class SharedAuthClient(
    private val httpClient: HttpClient,
    val authUrl: String,
    val authClientId: String,
    val authClientSecret: String
) {
    suspend fun fetchNewToken(scope: String): Result<OAuth2TokenData> {
        return safeApiCall {
            httpClient.submitForm(
                url = "$authUrl/oauth2/token",
                formParameters = parameters {
                    append("grant_type", "client_credentials")
                    append("client_id", authClientId)
                    append("client_secret", authClientSecret)
                    append("scope", scope)
                }
            ).asResource { body() }
        }
    }

    suspend fun fetchUserInfo(userId: String, accessToken: String): Result<UserResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/users/$userId") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }.asResource { body() }
        }
    }

    suspend fun fetchUsersInfo(
        userIds: List<String>,
        accessToken: String
    ): Result<Map<String, UserResponse>> {
        return safeApiCall {
            httpClient.get("$authUrl/users/batch") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                parameter("ids", userIds.joinToString(","))
            }.asResource { body<List<UserResponse>>().associateBy { it.id } }
        }
    }

    suspend fun getOrCreateUser(accessToken: String, req: CreateUserRequest): Result<UserResponse> {
        return safeApiCall {
            httpClient.patch("$authUrl/users") {
                setBody(req)
                headers { append(HttpHeaders.Authorization, "Bearer $accessToken") }
            }.asResource { body() }
        }
    }
}
