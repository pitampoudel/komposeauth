package com.vardansoft.komposeauth.core.config

import com.vardansoft.komposeauth.AppProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity
import org.springframework.security.web.webauthn.authentication.HttpSessionPublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations
import java.net.URL


@Configuration
class WebAuthnConfig(
    private val appProperties: AppProperties,
    val userContextService: UserContextService
) {
    @Bean
    fun requestOptionsRepository(): PublicKeyCredentialRequestOptionsRepository {
        return HttpSessionPublicKeyCredentialRequestOptionsRepository()
    }

    @Bean
    fun relyingPartyOperations(
        userCredentials: UserCredentialRepository,
        userEntities: PublicKeyCredentialUserEntityRepository
    ): WebAuthnRelyingPartyOperations {
        return Webauthn4JRelyingPartyOperations(
            userEntities,
            userCredentials,
            PublicKeyCredentialRpEntity.builder()
                .id(URL(appProperties.baseUrl()).host)
                .name(appProperties.name)
                .build(),
            setOf(appProperties.baseUrl)
        )
    }

    @Bean
    @Order(2)
    fun authNFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
    ): SecurityFilterChain {
        http.securityMatcher("/webauthn/**")
            .cors { }
            .csrf { it.disable() }
            .oauth2ResourceServer { conf ->
                conf.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .webAuthn {}
            .authorizeHttpRequests {
                it.anyRequest().authenticated()
            }
        return http.build()
    }

}
