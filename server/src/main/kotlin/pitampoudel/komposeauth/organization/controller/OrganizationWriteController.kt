package pitampoudel.komposeauth.organization.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.config.canEditOrganization
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.organization.data.CreateOrUpdateOrganizationRequest
import pitampoudel.komposeauth.organization.service.OrganizationService
import pitampoudel.komposeauth.organization.service.toOrganization
import pitampoudel.komposeauth.organization.service.updated
import java.util.UUID

@RestController
@Tag(name = "Organizations")
class OrganizationWriteController(
    private val storageService: StorageService,
    private val organizationService: OrganizationService,
    val userContextService: UserContextService
) {
    @PostMapping("/" + ApiEndpoints.ORGANIZATIONS)
    fun createOrUpdate(
        @RequestBody request: CreateOrUpdateOrganizationRequest
    ): MessageResponse {
        val user = userContextService.getUserFromAuthentication()

        // UPDATE ORGANIZATION
        request.orgId?.let { orgId ->
            val organization = organizationService.findById(orgId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found")

            val canEdit = canEditOrganization(organization, user)
            if (!canEdit) throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Insufficient permission"
            )

            // No logo in the request keeps the one already there, and the old file goes only once
            // the record points at its replacement.
            val newLogoUrl = uploadLogo(request)
            organizationService.save(
                organization.updated(
                    request = request,
                    logoImageUrl = newLogoUrl ?: organization.logoUrl,
                    oldEmail = organization.email,
                    oldEmailVerified = organization.emailVerified,
                    oldPhoneNumber = organization.phoneNumber,
                    oldPhoneNumberVerified = organization.phoneNumberVerified,
                )
            )
            if (newLogoUrl != null) organization.logoUrl?.let { storageService.delete(it) }

            return MessageResponse("Organization updated successfully")
        }

        // CREATE ORGANIZATION
        val organization = request.toOrganization(
            userId = user.id,
            logoImageUrl = uploadLogo(request),
        )
        organizationService.save(organization)
        return MessageResponse("Organization created successfully")
    }

    /** A name of its own per upload: a timestamp let two organizations saved in one second share a file. */
    private fun uploadLogo(request: CreateOrUpdateOrganizationRequest): String? =
        request.logo?.toKmpFile()?.let { file ->
            storageService.upload(
                blobName = "organization_logos/${UUID.randomUUID()}",
                contentType = file.mimeType,
                bytes = file.byteArray
            )
        }
}