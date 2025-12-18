package pitampoudel.komposeauth.organization.domain

import pitampoudel.core.data.MessageResponse
import pitampoudel.core.domain.KmpFile
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.organization.data.CreateOrUpdateOrganizationRequest
import pitampoudel.komposeauth.organization.data.OrganizationResponse

interface OrganizationsClient {
    suspend fun createOrUpdate(request: CreateOrUpdateOrganizationRequest): Result<MessageResponse>
    suspend fun get(): Result<List<OrganizationResponse>>
    suspend fun get(orgId: String): Result<OrganizationResponse>
    suspend fun delete(orgId: String): Result<MessageResponse>
    suspend fun download(url: String): Result<KmpFile>
}