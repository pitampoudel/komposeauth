package pitampoudel.komposeauth.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.util.UrlUtils
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.filter.JwtCookieAuthFilter
import pitampoudel.komposeauth.core.providers.OAuth2PublicClientAuthConverter
import pitampoudel.komposeauth.core.providers.OAuth2PublicClientAuthProvider
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.UserService
import java.net.URLEncoder
import java.time.Instant
import java.util.Base64
import javax.security.auth.login.AccountLockedException
import javax.security.auth.login.AccountNotFoundException

@Configuration
@EnableWebSecurity
class WebAuthorizationConfig() {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()


    @Bean
    fun userDetailsService(
        userService: UserService
    ): UserDetailsService = UserDetailsService { username ->
        val user = userService.findByUserName(username)
            ?: throw UsernameNotFoundException("User not found with username: $username")

        if (user.deactivated) {
            throw AccountLockedException("User account is deactivated")
        }
        if (user.email == null) {
            throw AccessDeniedException("User email is not verified")
        }

        User.withUsername(user.email)
            .password(user.passwordHash)
            .authorities(user.roles.map { role ->
                SimpleGrantedAuthority("ROLE_${role}")
            })
            .build()
    }

    class OAuth2RefreshTokenGenerator : OAuth2TokenGenerator<OAuth2RefreshToken> {
        private val refreshTokenGenerator: StringKeyGenerator = Base64StringKeyGenerator(
            Base64.getUrlEncoder().withoutPadding(), 96
        )

        override fun generate(context: OAuth2TokenContext): OAuth2RefreshToken? {
            if (OAuth2TokenType.REFRESH_TOKEN != context.tokenType) {
                return null
            }
            val issuedAt = Instant.now()
            val expiresAt =
                issuedAt.plus(context.registeredClient.tokenSettings.refreshTokenTimeToLive)
            return OAuth2RefreshToken(this.refreshTokenGenerator.generateKey(), issuedAt, expiresAt)
        }

    }

    @Bean
    fun tokenGenerator(
        jwtEncoder: JwtEncoder,
        jwtCustomizer: OAuth2TokenCustomizer<JwtEncodingContext>
    ): OAuth2TokenGenerator<*> {
        val jwtGenerator = JwtGenerator(jwtEncoder)
        jwtGenerator.setJwtCustomizer(jwtCustomizer)
        return DelegatingOAuth2TokenGenerator(
            jwtGenerator,
            OAuth2AccessTokenGenerator(),
            OAuth2RefreshTokenGenerator()
        )
    }


    @Bean
    fun jwtCustomizer(userService: UserService): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context ->
            when (context.authorizationGrantType) {
                AuthorizationGrantType.CLIENT_CREDENTIALS -> {
                    // val principal = context.getPrincipal<OAuth2ClientAuthenticationToken>()
                }

                else -> {
                    if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
                        val principal = context.getPrincipal<UsernamePasswordAuthenticationToken>()
                        val user = userService.findByUserName(principal.name)
                            ?: throw AccountNotFoundException("User not found with email: ${principal.name}")
                        context.claims.claim(
                            "authorities",
                            principal.authorities + user.roles.map { "ROLE_$it" }
                        )
                        context.claims.claim("first_name", user.firstName)
                        if (user.lastName != null)
                            context.claims.claim("last_name", user.lastName)
                    }
                }
            }

        }
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val authorities = jwt.getClaimAsStringList("authorities") ?: emptyList()
            val scopes = jwt.getClaimAsStringList("scope") ?: emptyList()
            val grantedAuthorities = mutableListOf<SimpleGrantedAuthority>()
            authorities.forEach { grantedAuthorities.add(SimpleGrantedAuthority(it)) }
            scopes.forEach { grantedAuthorities.add(SimpleGrantedAuthority("SCOPE_$it")) }
            grantedAuthorities.toList()
        }
        return converter
    }

    @Bean
    fun authorizationServerSettings(
        appConfigProvider: AppConfigProvider
    ): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder()
            .issuer(appConfigProvider.selfBaseUrl)
            .build()
    }

    @Bean
    fun securityContextRepository(): HttpSessionSecurityContextRepository =
        HttpSessionSecurityContextRepository()

    @Bean
    @Order(1)
    fun authFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        registeredClientRepository: RegisteredClientRepository,
        securityContextRepository: HttpSessionSecurityContextRepository,
        userService: UserService,
        kycService: KycService
    ): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()

        authorizationServerConfigurer.clientAuthentication {
            it.authenticationConverter(OAuth2PublicClientAuthConverter())
            it.authenticationProvider(
                OAuth2PublicClientAuthProvider(
                    registeredClientRepository = registeredClientRepository
                )
            )
        }

        return http.securityMatcher(authorizationServerConfigurer.endpointsMatcher)
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
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .securityContext {
                it.securityContextRepository(securityContextRepository)
            }
            .addFilterBefore(JwtCookieAuthFilter(), BearerTokenAuthenticationFilter::class.java)
            .oauth2ResourceServer { conf ->
                conf.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .authorizeHttpRequests {
                it.anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { request, response, _ ->
                    response.status = 302
                    response.setHeader(
                        "Location",
                        "/login-bridge.html?continue=${
                            URLEncoder.encode(
                                UrlUtils.buildFullRequestUrl(request), Charsets.UTF_8
                            )
                        }"
                    )
                }
            }
            .build()
    }
}
