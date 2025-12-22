package pitampoudel.komposeauth.core.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
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
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.core.domain.Constants.ACCESS_TOKEN_COOKIE_NAME
import pitampoudel.komposeauth.core.providers.OAuth2PublicClientAuthConverter
import pitampoudel.komposeauth.core.providers.OAuth2PublicClientAuthProvider
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.service.KycService
import pitampoudel.komposeauth.user.service.UserService
import java.time.Instant
import java.util.Base64
import javax.security.auth.login.AccountNotFoundException

@Configuration
@EnableWebSecurity
class WebAuthorizationConfig() {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    @Bean
    fun securityContextRepository() = HttpSessionSecurityContextRepository()

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
    fun bearerTokenResolver(): BearerTokenResolver {
        return BearerTokenResolver { request ->

            val delegate: BearerTokenResolver = DefaultBearerTokenResolver()
            // 1. Try Authorization header first
            val fromHeader = try {
                delegate.resolve(request)
            } catch (ex: Exception) {
                null // swallow it → public endpoints remain unaffected
            }
            if (!fromHeader.isNullOrBlank()) fromHeader else {
                // 2. Then try cookie (ACCESS_TOKEN)
                val cookie = request.cookies?.firstOrNull { it.name == ACCESS_TOKEN_COOKIE_NAME }
                cookie?.value
            }
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
    fun jwtCustomizer(
        userService: UserService,
        kycService: KycService
    ): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context ->
            when (context.authorizationGrantType) {
                AuthorizationGrantType.CLIENT_CREDENTIALS -> {
                    // val principal = context.getPrincipal<OAuth2ClientAuthenticationToken>()
                }

                else -> {
                    val principal = context.getPrincipal<UsernamePasswordAuthenticationToken>()
                    val user = userService.findByUserName(
                        principal.name
                    ) ?: throw AccountNotFoundException(
                        "User not found with email: ${principal.name}"
                    )
                    context.claims.claim("authorities", principal.authorities.map { it.authority })
                    user.email?.let {
                        context.claims.claim("email", it)
                    }
                    context.claims.claim("givenName", user.firstName)
                    user.lastName?.let {
                        context.claims.claim("familyName", it)
                    }
                    user.picture?.let {
                        context.claims.claim("picture", it)
                    }
                    context.claims.claim("kycVerified", kycService.isVerified(user.id))
                    context.claims.claim("phoneNumberVerified", user.phoneNumberVerified)
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
    @Order(1)
    fun authFilterChain(
        http: HttpSecurity,
        registeredClientRepository: RegisteredClientRepository,
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
                            val builder = OidcUserInfo.builder()
                                .claim("sub", user.id.toHexString())
                                .claim("emailVerified", user.emailVerified)
                                .claim("phoneNumberVerified", user.phoneNumberVerified)
                                .claim(
                                    "kycVerified",
                                    (kycService.find(user.id)?.status == KycResponse.Status.APPROVED)
                                )
                                .claim("givenName", user.firstName)
                                .claim("createdAt", user.createdAt.toString())
                                .claim("updatedAt", user.updatedAt.toString())
                                .claim("roles", user.roles)

                            user.email?.let {
                                builder.claim("email", user.email)
                            }
                            user.lastName?.let {
                                builder.claim("familyName", user.lastName)
                            }
                            user.picture?.let {
                                builder.claim("picture", user.picture)
                            }

                            builder.build()
                        }
                    }
                }
            }
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(
                    LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google")
                )
            }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()
            }
            .build()
    }
}
