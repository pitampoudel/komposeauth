package pitampoudel.komposeauth.core.config

import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.data.ApiEndpoints

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class WebSecurityConfig {
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
    @Order(2)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        cookieAwareBearerTokenResolver: BearerTokenResolver,
    ): SecurityFilterChain {
        return http
            .cors { }
            .csrf { csrf -> csrf.disable() }
            .securityContext { context ->
                context.securityContextRepository(HttpSessionSecurityContextRepository())
            }
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
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
                        "/login-bridge.html",
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
            .build()
    }
}
