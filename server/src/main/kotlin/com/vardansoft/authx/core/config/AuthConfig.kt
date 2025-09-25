package com.vardansoft.authx.core.config

import com.vardansoft.authx.core.converters.OAuth2PublicClientAuthConverter
import com.vardansoft.authx.core.providers.OAuth2PublicClientAuthProvider
import com.vardansoft.authx.data.ApiEndpoints
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.kyc.service.KycService
import com.vardansoft.authx.user.service.UserService
import jakarta.servlet.DispatcherType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class AuthConfig(
    val authSuccessHandler: AuthSuccessHandler,
    val jwtAuthenticationConverter: JwtAuthenticationConverter,
    val userService: UserService,
    val passwordEncoder: PasswordEncoder,
    val kycService: KycService
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
    @Order(2)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .cors { } // Apply global CORS configuration
            .csrf { csrf -> csrf.disable() }
            .oauth2ResourceServer { conf ->
                conf.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/oauth2/jwks",
                        "/login",
                        "/${ApiEndpoints.TOKEN}",
                        "/${ApiEndpoints.REFRESH_TOKEN}",
                        "/signup",
                        "/api/auth/**",
                        "/users",
                        "/assets/**",
                        "/config/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/verify-email",
                        "/reset-password"
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
                object : DaoAuthenticationProvider(UserDetailsService { email ->
                    val user = userService.findUserByEmailOrPhone(email)
                        ?: throw UsernameNotFoundException("User not found with email: $email")
                    User.withUsername(user.email)
                        .password(user.passwordHash)
                        .authorities(user.roles.map {
                            SimpleGrantedAuthority("ROLE_${it}")
                        })
                        .build()
                }) {
                    init {
                        setPasswordEncoder(this@AuthConfig.passwordEncoder)
                    }
                }
            )
            .build()
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = ["app.oauth-enabled"], havingValue = "true")
    fun authServerSecurityFilterChain(
        http: HttpSecurity,
        registeredClientRepository: RegisteredClientRepository
    ): SecurityFilterChain {
        val authorizationServerConfigurer: OAuth2AuthorizationServerConfigurer by lazy {
            OAuth2AuthorizationServerConfigurer.authorizationServer()
        }

        authorizationServerConfigurer.clientAuthentication {
            it.authenticationConverter(
                OAuth2PublicClientAuthConverter()
            )
            it.authenticationProvider(
                OAuth2PublicClientAuthProvider(
                    registeredClientRepository = registeredClientRepository
                )
            )
        }

        return http.securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .cors { } // Apply global CORS configuration
            .with(authorizationServerConfigurer) { authorizationServer ->
                authorizationServer.oidc {
                    it.userInfoEndpoint { userInfo ->
                        userInfo.userInfoMapper { context ->
                            val principal: Authentication = context.getAuthentication()
                            val userId = principal.name
                            val user = userService.findUser(userId) ?: throw IllegalStateException(
                                "User not found with id: $userId"
                            )
                            OidcUserInfo.builder()
                                .claim("id", user.id.toHexString())
                                .claim("email", user.email)
                                .claim("emailVerified", user.emailVerified)
                                .claim("phoneNumber", user.phoneNumber)
                                .claim("phoneNumberVerified", user.phoneNumberVerified)
                                .claim(
                                    "kycVerified",
                                    (kycService.find(user.id)?.status == KycResponse.Status.APPROVED)
                                )
                                .claim("socialLinks", user.socialLinks)
                                .claim("firstName", user.firstName)
                                .claim("lastName", user.lastName)
                                .claim("createdAt", user.createdAt.toString())
                                .claim("updatedAt", user.updatedAt.toString())
                                .claim("picture", user.picture)
                                .claim("roles", user.roles)
                                .build()
                        }
                    }
                }
            }
            .authorizeHttpRequests { auth ->
                // Specific OAuth2 server endpoint permissions
                auth.requestMatchers("/oauth2/token").permitAll()
                // All other OAuth2 endpoints covered by the matcher should be authenticated
                auth.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint("/login"),
                    MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            }
            .build()
    }

}
