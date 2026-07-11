package pitampoudel.komposeauth.core.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.core.domain.ApiEndpoints.THIRD_FACTOR_KYC
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class WebSecurityConfig {

    private fun clearTokenCookie(
        request: HttpServletRequest,
        response: HttpServletResponse,
        appConfigService: AppConfigService
    ) {
        val clearCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(request.isSecure)
            .path("/")
            .sameSite(if (request.isSecure) "None" else "Lax")
            .maxAge(0)
            .domain("." + appConfigService.rpId())
            .build()
        response.addHeader("Set-Cookie", clearCookie.toString())
    }

    @Bean
    fun corsConfigurationSource(appConfigService: AppConfigService): CorsConfigurationSource {
        return CorsConfigurationSource {
            val configuration = CorsConfiguration()
            val origins = appConfigService.corsAllowedOrigins()
            if (origins.any { it.contains("*") }) {
                configuration.allowedOriginPatterns = origins
            } else {
                configuration.allowedOrigins = origins
            }
            configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
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
        bearerTokenResolver: BearerTokenResolver,
        loginSuccessHandler: OAuth2LoginSuccessHandler,
        appConfigService: AppConfigService
    ): SecurityFilterChain {
        return http
            .cors { }
            .csrf { csrf -> csrf.disable() }
            .logout { logout ->
                logout
                    .logoutUrl("/${ApiEndpoints.LOGOUT}")
                    .logoutSuccessHandler { request, response, _ ->
                        clearTokenCookie(request, response, appConfigService)
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
                // An invalid/expired bearer token (from the Authorization header OR the access-token
                // cookie) is rejected directly by the resource-server filter, short-circuiting the
                // exceptionHandling entry point below. Clear the stale cookie here too so the browser
                // stops resending a token that will only keep producing 401s.
                val bearerEntryPoint = BearerTokenAuthenticationEntryPoint()
                conf.authenticationEntryPoint { request, response, authException ->
                    clearTokenCookie(request, response, appConfigService)
                    bearerEntryPoint.commence(request, response, authException)
                }
                conf.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .formLogin { formLogin ->
                formLogin
                    .loginPage("/session-login")
                    .loginProcessingUrl("/session-login")
                    .permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2.successHandler(loginSuccessHandler)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*PublicEndpoints.purelyPublicPatterns.toTypedArray()).permitAll()
                    .requestMatchers(*PublicEndpoints.optionalAuthPatterns.toTypedArray()).permitAll()
                    .requestMatchers(HttpMethod.POST, "/$THIRD_FACTOR_KYC").permitAll()
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                    ).hasRole("ADMIN")
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    { request, response, _ ->
                        clearTokenCookie(request, response, appConfigService)
                        response.status = 401
                        response.contentType = MediaType.APPLICATION_JSON_VALUE
                    },
                    NegatedRequestMatcher(MediaTypeRequestMatcher(MediaType.TEXT_HTML))
                )
            }
            .build()
    }
}
