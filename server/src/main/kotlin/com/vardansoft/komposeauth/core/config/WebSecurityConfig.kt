package com.vardansoft.komposeauth.core.config

import com.vardansoft.komposeauth.data.ApiEndpoints
import com.vardansoft.komposeauth.data.CreateUserRequest
import com.vardansoft.komposeauth.user.entity.User
import com.vardansoft.komposeauth.user.service.UserService
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

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
                user = userService.findByUserName(principal.username)
                    ?: throw IllegalStateException("User not found with username: ${principal.username}")
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
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class WebSecurityConfig(
    val passwordEncoder: PasswordEncoder,
    val userDetailsService: UserDetailsService,
    val authSuccessHandler: AuthSuccessHandler
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    @Order(3)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
    ): SecurityFilterChain {
        return http
            .cors { }
            .csrf { csrf -> csrf.disable() }
            .oauth2ResourceServer { conf ->
                conf.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/lib/**",
                        "/favicon.ico",
                        "/assets/**",
                        "/oauth2/jwks",
                        "/login",
                        "/${ApiEndpoints.TOKEN}",
                        "/signup",
                        "/api/auth/**",
                        "/users",
                        "/config/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/verify-email",
                        "/reset-password",
                        "/countries.json",
                        "/.well-known/**"
                    ).permitAll()
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form.loginPage("/login").successHandler(authSuccessHandler)
            }
            .oauth2Login { oauth2 ->
                oauth2.loginPage("/login").successHandler(authSuccessHandler)
            }
            .authenticationProvider(
                object : DaoAuthenticationProvider(userDetailsService) {
                    init {
                        setPasswordEncoder(this@WebSecurityConfig.passwordEncoder)
                    }
                }
            )
            .build()
    }
}
