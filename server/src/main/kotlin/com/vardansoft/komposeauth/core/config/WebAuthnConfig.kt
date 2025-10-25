package com.vardansoft.komposeauth.core.config

import com.vardansoft.komposeauth.AppProperties
import com.vardansoft.komposeauth.core.utils.WebAuthnUtils.webAuthnAllowedOrigins
import com.vardansoft.komposeauth.webauthn.PublicKeyCredential
import com.vardansoft.komposeauth.webauthn.PublicKeyCredentialRepository
import com.vardansoft.komposeauth.webauthn.PublicKeyUser
import com.vardansoft.komposeauth.webauthn.PublicKeyUserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.CredentialRecord
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.authentication.HttpSessionPublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations
import org.springframework.stereotype.Repository
import java.net.URL
import kotlin.jvm.optionals.getOrNull

@Repository
class UserCredentialRepositoryImpl(
    private val repository: PublicKeyCredentialRepository
) : UserCredentialRepository {
    override fun delete(credentialId: Bytes) {
        repository.deleteById(credentialId)
    }

    override fun save(credentialRecord: CredentialRecord) {
        repository.save(
            PublicKeyCredential(
                id = credentialRecord.credentialId,
                publicKeyUserId = credentialRecord.userEntityUserId,
                label = credentialRecord.label,
                attestationClientDataJSON = credentialRecord.attestationClientDataJSON,
                attestationObject = credentialRecord.attestationObject,
                signatureCount = credentialRecord.signatureCount,
                transports = credentialRecord.transports,
                publicKey = credentialRecord.publicKey,
                backupEligible = credentialRecord.isBackupEligible,
                backupState = credentialRecord.isBackupState,
                uvInitialized = credentialRecord.isUvInitialized,
                credentialType = credentialRecord.credentialType,
                lastUsed = credentialRecord.lastUsed
            )
        )
    }

    override fun findByCredentialId(credentialId: Bytes): CredentialRecord? {
        return repository.findById(credentialId).getOrNull()
    }

    override fun findByUserId(userId: Bytes): List<CredentialRecord> {
        return repository.findAllByPublicKeyUserId(userId)
    }

}

@Repository
class PublicKeyCredentialUserEntityRepositoryImpl(
    private val repository: PublicKeyUserRepository
) : PublicKeyCredentialUserEntityRepository {
    override fun findById(id: Bytes): PublicKeyCredentialUserEntity? {
        return repository.findById(id.toBase64UrlString()).getOrNull()
    }

    override fun findByUsername(username: String): PublicKeyCredentialUserEntity? {
        return repository.findByEmail(username)
    }

    override fun save(userEntity: PublicKeyCredentialUserEntity) {
        repository.save(
            PublicKeyUser(
                id = userEntity.id.toBase64UrlString(),
                email = userEntity.name,
                displayName = userEntity.displayName
            )
        )
    }

    override fun delete(id: Bytes) {
        repository.deleteById(id.toBase64UrlString())
    }

}

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
        val rpId = URL(appProperties.baseUrl()).host
        return Webauthn4JRelyingPartyOperations(
            userEntities,
            userCredentials,
            PublicKeyCredentialRpEntity.builder()
                .id(rpId)
                .name(appProperties.name)
                .build(),
            webAuthnAllowedOrigins(
                rpId = rpId
            ) + appProperties.baseUrl()
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
