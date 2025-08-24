package com.vardansoft.authx.core.config

import com.vardansoft.authx.user.entity.User
import com.vardansoft.authx.user.service.UserService
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service

@Service
class UserContextService(val userService: UserService) {
    fun getCurrentUser(): User {
        return when (val authentication = SecurityContextHolder.getContext().authentication) {
            is JwtAuthenticationToken -> {
                val jwt = authentication.principal as org.springframework.security.oauth2.jwt.Jwt
                if (jwt.subject.isNullOrEmpty() || jwt.claims.containsKey("client_id")) throw IllegalStateException(
                    "No user associated with authentication context"
                )

                userService.findUser(jwt.subject)
                    ?: throw IllegalStateException("User not found")
            }

            else -> throw IllegalStateException("Unsupported authentication type: ${authentication.javaClass.name}")
        }
    }

    fun authorities(): Collection<GrantedAuthority> {
        return SecurityContextHolder.getContext().authentication.authorities
    }

}