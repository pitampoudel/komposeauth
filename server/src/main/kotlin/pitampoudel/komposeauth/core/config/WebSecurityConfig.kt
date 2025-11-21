package pitampoudel.komposeauth.core.config

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
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.data.CreateUserRequest
import pitampoudel.komposeauth.config.service.AppConfigProvider
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.service.UserService

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
                        photoUrl = picture
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
    fun cookieAwareBearerTokenResolver(): BearerTokenResolver {
        val delegate = DefaultBearerTokenResolver()
        val publicPaths = setOf(
            "/${ApiEndpoints.LOGIN}",
            "/${ApiEndpoints.LOGIN_OPTIONS}",
        )

        return object : BearerTokenResolver {
            override fun resolve(request: HttpServletRequest): String? {
                val rawPath = request.servletPath ?: request.requestURI

                // Skip any public endpoint (starts with or exact match)
                if (publicPaths.any { rawPath.startsWith(it) }) {
                    return null
                }

                // 1. Try Authorization header first
                val fromHeader = delegate.resolve(request)
                if (!fromHeader.isNullOrBlank()) return fromHeader

                // 2. Then try cookie (ACCESS_TOKEN)
                val cookie = request.cookies?.firstOrNull { it.name == "ACCESS_TOKEN" }
                return cookie?.value
            }
        }
    }

    @Bean
    fun corsConfigurationSource(appConfigProvider: AppConfigProvider): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = appConfigProvider.corsAllowedOrigins()
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    @Order(3)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        cookieAwareBearerTokenResolver: BearerTokenResolver,
    ): SecurityFilterChain {
        return http
            .cors { }
            .csrf { csrf -> csrf.disable() }
            .oauth2ResourceServer { conf ->
                conf.bearerTokenResolver(cookieAwareBearerTokenResolver).jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/lib/**",
                        "/favicon.ico",
                        "/assets/**",
                        "/oauth2/jwks",
                        "/${ApiEndpoints.LOGIN}",
                        "/signup",
                        "/api/auth/**",
                        "/users",
                        "/${ApiEndpoints.LOGIN_OPTIONS}",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/${ApiEndpoints.VERIFY_EMAIL}",
                        "/reset-password",
                        "/countries.json",
                        "/.well-known/**",
                        "/setup"
                    ).permitAll()
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .anyRequest().authenticated()
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
