package com.vardansoft.auth.core.config


import com.vardansoft.auth.authorizations.service.MongoOAuth2AuthorizationService
import com.vardansoft.auth.core.converters.GoogleIdTokenGrantAuthConverter
import com.vardansoft.auth.core.converters.OAuth2PublicClientAuthConverter
import com.vardansoft.auth.core.providers.GoogleIdTokenGrantAuthProvider
import com.vardansoft.auth.core.providers.OAuth2PublicClientAuthProvider
import com.vardansoft.auth.user.service.UserService
import jakarta.servlet.DispatcherType
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class AuthConfig(
    val authSuccessHandler: AuthSuccessHandler,
    val jwtAuthenticationConverter: JwtAuthenticationConverter,
    val userService: UserService,
    val passwordEncoder: PasswordEncoder
) {
    @Bean
    @Order(2)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { csrf -> csrf.disable() }
            .oauth2ResourceServer { conf ->
                conf.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/oauth2/clients/**").hasRole("ADMIN")
                    .requestMatchers(
                        "/login", "/token", "/signup","/users", "/assets/**", "/reset-password"
                    ).permitAll()
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
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
                    val user = userService.findUserByEmail(email)
                        ?: throw UsernameNotFoundException("User not found with email: $email")
                    User.withUsername(user.email).password(user.passwordHash).build()
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
    fun authServerSecurityFilterChain(
        http: HttpSecurity,
        authorizationService: MongoOAuth2AuthorizationService,
        tokenGenerator: OAuth2TokenGenerator<*>,
        registeredClientRepository: RegisteredClientRepository,
        @Value("\${spring.security.oauth2.client.registration.google.client-id}")
        googleClientId: String
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

        // Configure token endpoint to support custom grant type
        authorizationServerConfigurer.tokenEndpoint { tokenEndpoint ->
            tokenEndpoint.accessTokenRequestConverter(
                GoogleIdTokenGrantAuthConverter()
            )
            tokenEndpoint.authenticationProvider(
                GoogleIdTokenGrantAuthProvider(
                    userService = userService,
                    authorizationService = authorizationService,
                    tokenGenerator = tokenGenerator,
                    registeredClientRepository = registeredClientRepository,
                    googleClientId = googleClientId
                )
            )
        }

        return http.securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .cors {
                it.configurationSource(UrlBasedCorsConfigurationSource().apply {
                    registerCorsConfiguration("/**", CorsConfiguration().apply {
                        allowedOrigins = listOf("*")
                        allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        allowedHeaders = listOf("*")
                    })
                })
            }
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
                                .claim("socialLinks", user.socialLinks)
                                .claim("firstName", user.firstName)
                                .claim("lastName", user.lastName)
                                .claim("createdAt", user.createdAt.toString())
                                .claim("updatedAt", user.updatedAt.toString())
                                .claim("picture", user.picture)
                                .build()
                        }
                    }
                }
            }.authorizeHttpRequests { auth ->
                auth.requestMatchers("/oauth2/token").permitAll()
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
