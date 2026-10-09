package pitampoudel.komposeauth.organization.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.config.canEditOrganization
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.organization.data.OrganizationResponse
import pitampoudel.komposeauth.organization.service.OrganizationService
import pitampoudel.komposeauth.organization.service.toApiResponse

@RestController
@Tag(name = "Organizations")
class OrganizationReadController(
    private val organizationService: OrganizationService,
    private val userContextService: UserContextService
) {

    @GetMapping("/" + ApiEndpoints.ORGANIZATIONS)
    fun getOrganizations(
        @RequestParam("ids")
        ids: String? = null
    ): List<OrganizationResponse> {
        val user = userContextService.getUserFromAuthentication()

        // An organization's contact details are its members' business, whichever way it is asked for.
        val organizations = if (!ids.isNullOrBlank()) {
            organizationService.findOrgs(ids.split(",")).filter { canEditOrganization(it, user) }
        } else {
            organizationService.findOrgsForUser(user.id)
        }
        return organizations.map { org ->
            org.toApiResponse(
                org.phoneNumber?.let { phone -> parsePhoneNumber(null, phone) }
            )
        }
    }

    @GetMapping("/" + ApiEndpoints.ORGANIZATIONS + "/{orgId}")
    fun getOrganizationById(
        @PathVariable orgId: String
    ): OrganizationResponse {
        val user = userContextService.getUserFromAuthentication()
        val organization = organizationService.findById(orgId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found")
        if (!canEditOrganization(organization, user)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission")
        }

        val phoneNumber = organization.phoneNumber?.let { phone ->
            parsePhoneNumber(null, phone)
        }

        return organization.toApiResponse(phoneNumber)
    }
}