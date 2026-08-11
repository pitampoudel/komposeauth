package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.domain.ApiEndpoints.ROLES
import pitampoudel.komposeauth.core.domain.ApiEndpoints.USERS
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.data.RoleResponse
import pitampoudel.komposeauth.user.data.UserResponse
import pitampoudel.komposeauth.user.service.UserService
import pitampoudel.komposeauth.user.service.mapToResponseDto

@RestController
class RolesController(
    private val userService: UserService,
    private val kycService: KycService,
    private val userContextService: UserContextService
) {

    @GetMapping("/$ROLES")
    @Operation(
        summary = "List roles",
        description = "List every grantable role — the built-in ones plus those configured in the app's role catalog — with the number of users holding each."
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    fun listRoles(): ResponseEntity<List<RoleResponse>> {
        return ResponseEntity.ok(userService.listRoles())
    }

    @PostMapping("/$USERS/{id}/$ROLES/{role}")
    @Operation(
        summary = "Grant role",
        description = "Grant a role to a user. The role must be in the app's role catalog. Granting SUPER_ADMIN requires SUPER_ADMIN."
    )
    @Parameter(name = "id", description = "User ID", required = true)
    @Parameter(name = "role", description = "Role name", required = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    fun grant(
        @PathVariable id: String,
        @PathVariable role: String,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {
        val actor = userContextService.getUserFromAuthentication(authentication)
        val user = userService.grantRole(actor, id, role)
        return ResponseEntity.ok(user.mapToResponseDto(kycService.isVerified(user.id)))
    }

    @DeleteMapping("/$USERS/{id}/$ROLES/{role}")
    @Operation(
        summary = "Revoke role",
        description = "Revoke a role from a user. Will fail if it is the last holder of a protected role such as ADMIN. Revoking SUPER_ADMIN requires SUPER_ADMIN."
    )
    @Parameter(name = "id", description = "User ID", required = true)
    @Parameter(name = "role", description = "Role name", required = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    fun revoke(
        @PathVariable id: String,
        @PathVariable role: String,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {
        val actor = userContextService.getUserFromAuthentication(authentication)
        val user = userService.revokeRole(actor, id, role)
        return ResponseEntity.ok(user.mapToResponseDto(kycService.isVerified(user.id)))
    }
}
