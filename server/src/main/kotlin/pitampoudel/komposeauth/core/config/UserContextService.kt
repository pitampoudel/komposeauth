package pitampoudel.komposeauth.core.config

import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService

@Service
class UserContextService(val userService: UserService) {
    /**
     * The signed-in user, or a 401: a token for a client acting as itself, a user who has since been
     * deleted or deactivated and a missing login are all the caller's to fix, not a server error.
     */
    fun getUserFromAuthentication(authentication: Authentication? = SecurityContextHolder.getContext().authentication): User {
        val username = when (authentication) {
            is JwtAuthenticationToken -> authentication.token.takeUnless { it.hasClaim("client_id") }?.subject
            is UsernamePasswordAuthenticationToken -> authentication.name
            else -> null
        }
        return username?.takeIf { it.isNotEmpty() }?.let { userService.findByUserName(it) }?.takeUnless { it.deactivated }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.")
    }

    fun authenticatedUserOrNull(): User? {
        return runCatching { getUserFromAuthentication() }.getOrNull()
    }
}

/** SUPER_ADMIN is a strict superset of ADMIN, the same way the granted-authority hierarchy has it. */
fun User.isAdmin() = roles.any { it == Roles.ADMIN || it == Roles.SUPER_ADMIN }

fun canEditOrganization(organization: Organization, user: User): Boolean =
    user.isAdmin() || user.id in organization.userIds
