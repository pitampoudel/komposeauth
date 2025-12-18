package pitampoudel.komposeauth.organization.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import pitampoudel.core.data.MessageResponse
import pitampoudel.core.data.asResource
import pitampoudel.core.data.download
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.data.ApiEndpoints
import pitampoudel.komposeauth.organization.domain.OrganizationsClient

internal class OrganizationsClientImpl(
    private val httpClient: HttpClient,
    val baseUrl: String
) : OrganizationsClient {
    override suspend fun createOrUpdate(
        request: CreateOrUpdateOrganizationRequest,
    ): Result<MessageResponse> {
        return safeApiCall {
            httpClient.post(baseUrl + "/" + ApiEndpoints.ORGANIZATION) {
                setBody(request)
            }.asResource {
                body()
            }

        }
    }

    override suspend fun get(orgId: String): Result<OrganizationResponse> {
        return safeApiCall {
            httpClient.get(
                baseUrl + "/" + ApiEndpoints.ORGANIZATION + "/$orgId"
            ).asResource { body() }
        }
    }

    override suspend fun delete(orgId: String): Result<MessageResponse> {
        return safeApiCall {
            httpClient.delete(baseUrl + "/" + ApiEndpoints.ORGANIZATION) {
                parameter("orgId", orgId)
            }.asResource { body() }
        }
    }

    override suspend fun download(url: String) = httpClient.download(url)
}