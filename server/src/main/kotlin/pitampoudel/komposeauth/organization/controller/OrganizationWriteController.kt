package pitampoudel.komposeauth.organization.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.MessageResponse
import pitampoudel.core.domain.now
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.config.canEditOrganization
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.organization.data.CreateOrUpdateOrganizationRequest
import pitampoudel.komposeauth.organization.service.OrganizationService
import pitampoudel.komposeauth.organization.service.toOrganization
import pitampoudel.komposeauth.organization.service.updated

@RestController
@Tag(name = "Organizations")
class OrganizationWriteController(
    private val storageService: StorageService,
    private val organizationService: OrganizationService,
    val userContextService: UserContextService
) {
    @PostMapping("/" + ApiEndpoints.ORGANIZATION)
    suspend fun createOrUpdate(
        @RequestBody request: CreateOrUpdateOrganizationRequest
    ): MessageResponse {
        val user = userContextService.getUserFromAuthentication()

        // UPDATE ORGANIZATION
        request.orgId?.let { orgId ->
            val organization = organizationService.findById(orgId)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization not found")

            val canEdit = canEditOrganization(organization, user)
            if (!canEdit) throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Insufficient permission"
            )

            if (organization.logoUrl != null) {
                storageService.delete(organization.logoUrl)
            }

            val logoImageUrl = request.logo?.toKmpFile()?.let { file ->
                storageService.upload(
                    blobName = "organization_logos/${now().epochSeconds}",
                    contentType = file.mimeType,
                    bytes = file.byteArray
                )
            }

            organizationService.save(
                organization.updated(
                    request = request,
                    logoImageUrl = logoImageUrl,
                    oldEmail = organization.email,
                    oldEmailVerified = organization.emailVerified,
                    oldPhoneNumber = organization.phoneNumber,
                    oldPhoneNumberVerified = organization.phoneNumberVerified,
                )
            )

            return MessageResponse("Organization updated successfully")
        }

        // CREATE ORGANIZATION
        val logoImageUrl = request.logo?.toKmpFile()?.let { file ->
            storageService.upload(
                blobName = "organization_logos/${now().epochSeconds}",
                contentType = file.mimeType,
                bytes = file.byteArray
            )
        }

        val organization = request.toOrganization(
            userId = user.id,
            logoImageUrl = logoImageUrl,
        )
        organizationService.save(organization)
        return MessageResponse("Organization created successfully")
    }
}