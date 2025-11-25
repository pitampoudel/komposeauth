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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.filter.JwtCookieAuthFilter
import pitampoudel.komposeauth.data.ApiEndpoints

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
        jwtAuthenticationConverter: JwtAuthenticationConverter,
    ): SecurityFilterChain {
        return http
            .cors { }
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(JwtCookieAuthFilter(), BearerTokenAuthenticationFilter::class.java)
            .oauth2ResourceServer { conf ->
                conf.jwt {
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
