package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pitampoudel.core.data.MessageResponse
import pitampoudel.core.data.PageResponse
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.core.utils.findCurrentBaseUrl
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.data.ApiEndpoints.ME
import pitampoudel.komposeauth.data.ApiEndpoints.STATS
import pitampoudel.komposeauth.data.ApiEndpoints.USERS
import pitampoudel.komposeauth.data.CreateUserRequest
import pitampoudel.komposeauth.data.ProfileResponse
import pitampoudel.komposeauth.data.StatsResponse
import pitampoudel.komposeauth.data.UpdateProfileRequest
import pitampoudel.komposeauth.data.UserResponse
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.oauth_clients.entity.OAuth2Client.Companion.SCOPE_READ_ANY_USER
import pitampoudel.komposeauth.user.dto.mapToProfileResponseDto
import pitampoudel.komposeauth.user.dto.mapToResponseDto
import pitampoudel.komposeauth.user.service.UserService
import kotlin.time.ExperimentalTime


@RestController
class UsersController(
    val userService: UserService,
    val kycService: KycService,
    private val userContextService: UserContextService
) {
    @PostMapping("/$USERS")
    @Operation(
        summary = "Create user",
        description = "Creates a new user account",
    )
    fun create(
        @RequestBody request: CreateUserRequest,
        req: HttpServletRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok()
            .body(
                userService.createUser(findCurrentBaseUrl(req), request).mapToResponseDto(false)
            )

    }

    @PatchMapping("/$USERS")
    @Operation(
        summary = "Create a user or return existing",
        description = "Creates a new user account or returns existing user",
    )
    fun findOrCreateUser(
        @RequestBody request: CreateUserRequest,
        req: HttpServletRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userService.findOrCreateUser(findCurrentBaseUrl(req), request).let {
                it.mapToResponseDto(kycService.isVerified(it.id))
            }
        )
    }

    @GetMapping("/$USERS/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Fetch a single user by their ID"
    )
    @Parameter(name = "id", description = "User ID", required = true)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_$SCOPE_READ_ANY_USER')")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserResponse> {
        val user = userService.findUser(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user.mapToResponseDto(kycService.isVerified(user.id)))
    }

    @GetMapping("/$USERS")
    @Operation(
        summary = "Get users",
        description = "Fetch users by optional filters: comma-separated IDs (ids), search query (q). If no filters provided, returns all users with pagination."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_$SCOPE_READ_ANY_USER')")
    fun getUsers(
        @RequestParam(required = false) ids: String?,
        @RequestParam(required = false, name = "q") query: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "50") size: Int
    ): ResponseEntity<PageResponse<UserResponse>> {
        val idList = ids?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        val usersPage = userService.findUsersFlexible(idList, query, page, size)
        val userResponses = usersPage.content.map {
            it.mapToResponseDto(kycService.isVerified(it.id))
        }
        val body = PageResponse(
            items = userResponses,
            page = usersPage.number,
            pageSize = usersPage.size,
            totalItems = usersPage.totalElements,
            hasNext = usersPage.hasNext()
        )
        return ResponseEntity.ok(body)
    }

    @GetMapping("/$ME")
    @Operation(
        summary = "Get user profile",
        description = "Returns user information for the authenticated user"
    )
    fun getUserProfile(authentication: Authentication): ResponseEntity<ProfileResponse> {
        val userId = authentication.name
        val user = userService.findUser(userId) ?: return ResponseEntity.notFound().build()
        val userInfo =
            user.mapToProfileResponseDto(kycService.isVerified(user.id))
        return ResponseEntity.ok(userInfo)
    }

    @PostMapping("/${ApiEndpoints.DEACTIVATE}")
    @Operation(
        summary = "Deactivate account",
        description = "Deactivates the currently authenticated user's account."
    )
    fun deactivate(): ResponseEntity<MessageResponse> {
        val user = userContextService.getUserFromAuthentication()
        userService.deactivateUser(user.id)
        return ResponseEntity.ok(MessageResponse("User account deactivated successfully"))
    }

    @PostMapping("/${ApiEndpoints.UPDATE_PROFILE}")
    @Operation(
        summary = "Update current user information"
    )
    fun update(@RequestBody request: UpdateProfileRequest): ResponseEntity<ProfileResponse> {
        val user = userContextService.getUserFromAuthentication()
        return ResponseEntity.ok(userService.updateUser(user.id, request))
    }

    @GetMapping("/$STATS")
    @Operation(
        summary = "Get stats"
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_$SCOPE_READ_ANY_USER')")
    fun getStats(): ResponseEntity<StatsResponse> {
        return ResponseEntity.ok(
            StatsResponse(
                totalUsers = userService.countUsers()
            )
        )
    }

}