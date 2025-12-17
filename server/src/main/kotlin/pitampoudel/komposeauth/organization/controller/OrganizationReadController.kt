package pitampoudel.komposeauth.organization.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.websocket.server.PathParam
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.data.ApiEndpoints
import pitampoudel.komposeauth.organization.data.OrganizationResponse
import pitampoudel.komposeauth.organization.service.OrganizationService
import pitampoudel.komposeauth.organization.service.toApiResponse

@RestController
@Tag(name = "Organizations")
class OrganizationReadController(
    private val organizationService: OrganizationService,
    val userContextService: UserContextService
) {

    @GetMapping("/" + ApiEndpoints.ORGANIZATION)
    suspend fun getOrganizations(): List<OrganizationResponse> {
        val user = userContextService.getUserFromAuthentication()
        val organizations = organizationService.findOrgsForUser(user.id)
        return organizations.map { org ->
            org.toApiResponse(
                org.phoneNumber?.let { phone -> parsePhoneNumber(null, phone) }
            )
        }
    }

    @GetMapping("/" + ApiEndpoints.ORGANIZATION + "/{orgId}")
    suspend fun getOrganizationById(
        @PathParam("orgId") orgId: String
    ): OrganizationResponse {
        val organization = organizationService.findById(orgId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization not found")

        val phoneNumber = organization.phoneNumber?.let { phone ->
            parsePhoneNumber(null, phone)
        }

        return organization.toApiResponse(phoneNumber)
    }
}