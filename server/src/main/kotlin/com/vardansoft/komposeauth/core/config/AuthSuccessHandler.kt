package com.vardansoft.komposeauth.core.config

import com.vardansoft.komposeauth.data.CreateUserRequest
import com.vardansoft.komposeauth.user.entity.User
import com.vardansoft.komposeauth.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class AuthSuccessHandler(
    val userService: UserService
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val user: User
        when (val principal = authentication.principal) {
            is OAuth2User -> {
                val email: String = principal.getAttribute<String>("email").orEmpty()
                val firstName: String = principal.getAttribute<String>("given_name").orEmpty()
                val lastName: String = principal.getAttribute<String>("family_name").orEmpty()
                val picture: String = principal.getAttribute<String>("picture").orEmpty()
                val emailVerified: Boolean =
                    principal.getAttribute<Boolean>("emailVerified") == true
                user = userService.findOrCreateUser(
                    CreateUserRequest(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        picture = picture
                    )
                )
                if (emailVerified) userService.emailVerified(user.id)
            }

            is UserDetails -> {
                user = userService.findUserByEmailOrPhone(principal.username)
                    ?: throw IllegalStateException("User not found with email: ${principal.username}")
            }

            else -> {
                throw IllegalStateException(
                    "Authenticated not supported for: ${authentication.principal.javaClass.name}"
                )
            }
        }

        val auth = user.asAuthToken()
        SecurityContextHolder.getContext().authentication = auth

        SavedRequestAwareAuthenticationSuccessHandler().onAuthenticationSuccess(
            request,
            response,
            auth
        )
    }
}