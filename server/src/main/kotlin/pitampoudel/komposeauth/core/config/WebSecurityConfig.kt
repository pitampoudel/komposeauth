package pitampoudel.komposeauth.core.config

import jakarta.servlet.DispatcherType
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
import org.springframework.security.web.util.UrlUtils
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.data.ApiEndpoints
import java.net.URLEncoder

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class WebSecurityConfig {

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
        jwtAuthenticationConverter: JwtAuthenticationConverter
    ): SecurityFilterChain {
        return http
            .cors { }
            .csrf { csrf -> csrf.disable() }
            .logout { logout ->
                logout
                    .deleteCookies("ACCESS_TOKEN")
                    .invalidateHttpSession(true)
            }
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .oauth2ResourceServer { conf ->
                conf.bearerTokenResolver { request ->
                    val delegate: BearerTokenResolver = DefaultBearerTokenResolver()
                    // 1. Try Authorization header first
                    val fromHeader = try {
                        delegate.resolve(request)
                    } catch (ex: Exception) {
                        null // swallow it → public endpoints remain unaffected
                    }
                    return@bearerTokenResolver if (!fromHeader.isNullOrBlank()) fromHeader else {
                        // 2. Then try cookie (ACCESS_TOKEN)
                        val cookie = request.cookies?.firstOrNull { it.name == "ACCESS_TOKEN" }
                        cookie?.value
                    }
                }
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
                        "/login-bridge.html",
                        "/oauth2/jwks",
                        "/${ApiEndpoints.LOGIN}",
                        "/${ApiEndpoints.LOGIN}/session",
                        "/signup",
                        "/api/auth/**",
                        "/users",
                        "/${ApiEndpoints.LOGIN_OPTIONS}",
                        "/${ApiEndpoints.VERIFY_EMAIL}",
                        "/reset-password",
                        "/countries.json",
                        "/.well-known/**",
                        "/setup"
                    ).permitAll()
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                    ).hasAnyRole("DEVELOPER", "ADMIN", "SUPER_ADMIN")
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { request, response, authException ->
                    val accept = request.getHeader("Accept") ?: ""
                    val wantsHtml = accept.contains("text/html", ignoreCase = true)
                    if (wantsHtml) {
                        val continueUrl = URLEncoder.encode(
                            UrlUtils.buildFullRequestUrl(request), Charsets.UTF_8
                        )
                        response.sendRedirect("/login-bridge.html?continue=$continueUrl")
                    } else {
                        response.sendError(401, authException?.message)
                    }
                }
            }
            .build()
    }
}
