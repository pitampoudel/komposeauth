package pitampoudel.komposeauth.organization.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.config.canEditOrganization
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.organization.service.OrganizationService

@RestController
@Tag(name = "Organizations")
class OrganizationDeleteController(
    private val organizationService: OrganizationService,
    private val storageService: StorageService,
    private val userContextService: UserContextService
) {
    @DeleteMapping("/" + ApiEndpoints.ORGANIZATIONS + "/{orgId}")
    fun deleteOrganization(
        @PathVariable orgId: String
    ): MessageResponse {
        val user = userContextService.getUserFromAuthentication()

        val organization = organizationService.findById(orgId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found")

        val canEdit = canEditOrganization(organization, user)
        if (!canEdit) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission")

        organizationService.delete(organization.id)
        organization.logoUrl?.let { storageService.delete(it) }

        return MessageResponse("Organization deleted")
    }
}