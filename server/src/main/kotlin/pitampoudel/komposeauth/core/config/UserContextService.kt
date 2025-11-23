package pitampoudel.komposeauth.core.config

import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service

@Service
class UserContextService(val userService: UserService) {
    fun getUserFromAuthentication(authentication: Authentication? = SecurityContextHolder.getContext().authentication): User {
        return when (authentication) {
            is JwtAuthenticationToken -> {
                val jwt = authentication.principal as org.springframework.security.oauth2.jwt.Jwt
                if (jwt.subject.isNullOrEmpty() || jwt.claims.containsKey("client_id")) throw IllegalStateException(
                    "No user associated with authentication context"
                )

                userService.findByUserName(jwt.subject)
                    ?: throw IllegalStateException("User not found")
            }

            is UsernamePasswordAuthenticationToken -> {
                authentication.name.let {
                    userService.findByUserName(it)
                } ?: throw IllegalStateException(
                    "No user associated with authentication context"
                )
            }


            else -> throw IllegalStateException("Unsupported authentication type: ${authentication?.javaClass?.name}")
        }
    }

}
