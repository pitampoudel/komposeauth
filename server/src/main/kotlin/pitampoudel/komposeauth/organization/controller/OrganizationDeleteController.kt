package pitampoudel.komposeauth.organization.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.config.canEditOrganization
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.organization.service.OrganizationService

@RestController
@Tag(name = "Organizations")
class OrganizationDeleteController(
    private val organizationService: OrganizationService,
    private val storageService: StorageService,
    private val userContextService: UserContextService
) {
    @DeleteMapping("/" + ApiEndpoints.ORGANIZATION)
    suspend fun deleteOrganization(
        @RequestParam("orgId")
        orgId: String
    ): MessageResponse {
        val user = userContextService.getUserFromAuthentication()

        val organization = organizationService.findById(orgId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "organization not found")

        val canEdit = canEditOrganization(organization, user)
        if (!canEdit) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission")

        organization.logoUrl?.let { storageService.delete(it) }
        organizationService.delete(organization.id)

        return MessageResponse("Organization deleted")
    }
}