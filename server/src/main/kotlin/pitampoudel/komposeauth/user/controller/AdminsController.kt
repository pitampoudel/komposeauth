package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.core.data.PageResponse
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.user.data.UserResponse
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.mapToResponseDto
import pitampoudel.komposeauth.user.service.UserService

@RestController
class AdminsController(
    private val userService: UserService,
    private val kycService: KycService,
    val userContextService: UserContextService

) {

    @GetMapping("/admins")
    @Operation(summary = "List admins", description = "List all users with ADMIN role (paginated)")
    @PreAuthorize("hasRole('ADMIN')")
    fun listAdmins(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "50") size: Int
    ): ResponseEntity<PageResponse<UserResponse>> {
        val result = userService.listAdmins(page, size)
        val verifiedUserIds = kycService.verifiedUserIds(result.content.map { it.id })
        val items = result.content.map { user ->
            user.mapToResponseDto(verifiedUserIds.contains(user.id))
        }
        return ResponseEntity.ok(
            PageResponse(
                items = items,
                page = result.number,
                pageSize = result.size,
                totalItems = result.totalElements,
                hasNext = result.hasNext()
            )
        )
    }

    @PostMapping("/admins/{id}")
    @Operation(summary = "Grant admin", description = "Grant ADMIN role to a user")
    @PreAuthorize("hasRole('ADMIN')")
    fun grant(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {
        val actor = userContextService.getUserFromAuthentication(authentication)
        val user = userService.grantAdmin(actor.fullName, id)
        return ResponseEntity.ok(user.mapToResponseDto(kycService.isVerified(user.id)))
    }

    @DeleteMapping("/admins/{id}")
    @Operation(
        summary = "Revoke admin",
        description = "Revoke ADMIN role from a user. Will fail if it is the last admin."
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun revoke(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {
        val actor = userContextService.getUserFromAuthentication(authentication)
        val user = userService.revokeAdmin(actor.fullName, id)
        return ResponseEntity.ok(user.mapToResponseDto(kycService.isVerified(user.id)))
    }
}
