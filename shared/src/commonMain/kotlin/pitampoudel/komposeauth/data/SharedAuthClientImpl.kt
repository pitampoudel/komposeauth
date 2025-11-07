package pitampoudel.komposeauth.data


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
import pitampoudel.core.data.asResource
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.domain.SharedAuthClient

class SharedAuthClientImpl(
    private val httpClient: HttpClient,
    val authUrl: String,
    val authClientId: String,
    val authClientSecret: String
) : SharedAuthClient {
    override suspend fun fetchNewToken(scope: String): Result<OAuth2Response> {
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

    override suspend fun fetchUserInfo(userId: String, accessToken: String): Result<UserResponse> {
        return safeApiCall {
            httpClient.get("$authUrl/${ApiEndpoints.USERS}/$userId") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }.asResource { body() }
        }
    }

    override suspend fun fetchUsersInfo(
        userIds: List<String>,
        accessToken: String
    ): Result<Map<String, UserResponse>> {
        return safeApiCall {
            httpClient.get("$authUrl/${ApiEndpoints.USERS_IN_BULK}") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                parameter("ids", userIds.joinToString(","))
            }.asResource { body<List<UserResponse>>().associateBy { it.id } }
        }
    }

    override suspend fun getOrCreateUser(
        accessToken: String,
        req: CreateUserRequest
    ): Result<UserResponse> {
        return safeApiCall {
            httpClient.patch("$authUrl/${ApiEndpoints.USERS}") {
                setBody(req)
                headers { append(HttpHeaders.Authorization, "Bearer $accessToken") }
            }.asResource { body() }
        }
    }
}
