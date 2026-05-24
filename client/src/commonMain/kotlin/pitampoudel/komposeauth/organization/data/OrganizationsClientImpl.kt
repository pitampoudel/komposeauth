package pitampoudel.komposeauth.organization.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import pitampoudel.core.data.MessageResponse
import pitampoudel.core.data.asResource
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.Config
import pitampoudel.komposeauth.organization.domain.OrganizationsClient

internal class OrganizationsClientImpl(
    private val httpClient: HttpClient
) : OrganizationsClient {
    val baseUrl: String? get() = Config.authServerUrl

    override suspend fun createOrUpdate(
        request: CreateOrUpdateOrganizationRequest,
    ): Result<MessageResponse> {
        return safeApiCall {
            httpClient.post(baseUrl + "/" + ApiEndpoints.ORGANIZATIONS) {
                setBody(request)
            }.asResource {
                body()
            }

        }
    }

    override suspend fun get(): Result<List<OrganizationResponse>> {
        return safeApiCall {
            httpClient.get(
                baseUrl + "/" + ApiEndpoints.ORGANIZATIONS
            ).asResource { body() }
        }
    }

    override suspend fun get(orgId: String): Result<OrganizationResponse> {
        return safeApiCall {
            httpClient.get(
                baseUrl + "/" + ApiEndpoints.ORGANIZATIONS + "/$orgId"
            ).asResource { body() }
        }
    }

    override suspend fun delete(orgId: String): Result<MessageResponse> {
        return safeApiCall {
            httpClient.delete(baseUrl + "/" + ApiEndpoints.ORGANIZATIONS) {
                parameter("orgId", orgId)
            }.asResource { body() }
        }
    }

}