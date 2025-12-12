package pitampoudel.komposeauth.core.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.UrlUtils
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.data.Constants.ACCESS_TOKEN_COOKIE_NAME
import java.net.URLEncoder

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class WebSecurityConfig {

    @Bean
    fun corsConfigurationSource(appConfigProvider: AppConfigProvider): CorsConfigurationSource {
        return CorsConfigurationSource {
            val configuration = CorsConfiguration()
            val origins = appConfigProvider.corsAllowedOrigins()
            if (origins.any { it.contains("*") }) {
                configuration.allowedOriginPatterns = origins
            } else {
                configuration.allowedOrigins = origins
            }
            configuration.allowedOrigins = appConfigProvider.corsAllowedOrigins()
            configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            configuration.allowedHeaders = listOf("*")
            configuration.allowCredentials = true
            configuration
        }
    }

    @Bean
    @Order(2)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        objectMapper: ObjectMapper,
        bearerTokenResolver: BearerTokenResolver
    ): SecurityFilterChain {
        return http
            .cors { }
            .csrf { csrf -> csrf.disable() }
            .logout { logout ->
                logout
                    .logoutUrl("/${ApiEndpoints.LOGOUT}")
                    .logoutSuccessHandler { request, response, _ ->
                        val clearCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                            .httpOnly(true)
                            .secure(request.isSecure)
                            .path("/")
                            .sameSite(if (request.isSecure) "None" else "Lax")
                            .maxAge(0)
                            .build()
                        response.addHeader("Set-Cookie", clearCookie.toString())
                        response.contentType = MediaType.APPLICATION_JSON_VALUE
                        response.writer.write(
                            objectMapper.writeValueAsString(
                                MessageResponse(message = "Logout successful")
                            )
                        )
                    }
            }
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .oauth2ResourceServer { conf ->
                conf.bearerTokenResolver(bearerTokenResolver)
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
                        "/${ApiEndpoints.LOGOUT}",
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
                    ).hasRole("ADMIN")
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
