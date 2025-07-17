package com.vardansoft.auth.core.config

import com.vardansoft.auth.user.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.lang.Nullable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.*
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.*

@Configuration
class SecurityConfig() {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()


    class OAuth2RefreshTokenGenerator : OAuth2TokenGenerator<OAuth2RefreshToken> {
        private val refreshTokenGenerator: StringKeyGenerator = Base64StringKeyGenerator(
            Base64.getUrlEncoder().withoutPadding(), 96
        )

        @Nullable
        override fun generate(context: OAuth2TokenContext): OAuth2RefreshToken? {
            if (OAuth2TokenType.REFRESH_TOKEN != context.tokenType) {
                return null
            }
            val issuedAt = Instant.now()
            val expiresAt = issuedAt.plus(context.registeredClient.tokenSettings.refreshTokenTimeToLive)
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
                    val principal = context.getPrincipal<OAuth2ClientAuthenticationToken>()

                }

                else -> {
                    val principal = context.getPrincipal<UsernamePasswordAuthenticationToken>()
                    if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
                        context.claims.claim("authorities", principal.authorities.map { it.authority })
                        val user = userService.findUser(principal.name)
                        context.claims.claim("first_name", user?.firstName)
                        context.claims.claim("last_name", user?.lastName)

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
        @Value("\${app.baseUrl}")
        baseUrl: String
    ): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder()
            .issuer(baseUrl)
            .build()
    }

}
