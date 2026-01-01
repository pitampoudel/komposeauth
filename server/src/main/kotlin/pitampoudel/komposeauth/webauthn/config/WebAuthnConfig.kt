package pitampoudel.komposeauth.webauthn.config

import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
import org.springframework.security.web.webauthn.registration.HttpSessionPublicKeyCredentialCreationOptionsRepository
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.webauthn.entity.PublicKeyCredential
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser
import pitampoudel.komposeauth.webauthn.repository.PublicKeyCredentialRepository
import pitampoudel.komposeauth.webauthn.repository.PublicKeyUserRepository
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.jvm.optionals.getOrNull

private fun stableWebAuthnUserHandle(userId: ObjectId): Bytes {
    // 32 bytes, stable. Purpose-separated so it doesn't collide with other hashes.
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes =
        digest.digest("komposeauth:webauthn:userHandle:${userId.toHexString()}".toByteArray(StandardCharsets.UTF_8))
    return Bytes(bytes)
}

@Repository
class UserCredentialRepositoryImpl(
    private val repository: PublicKeyCredentialRepository,
    private val publicKeyUserRepository: PublicKeyUserRepository
) : UserCredentialRepository {
    override fun delete(credentialId: Bytes) {
        repository.deleteById(credentialId)
    }

    override fun save(credentialRecord: CredentialRecord) {
        // Best-effort denormalization: map userHandle -> userId.
        val publicKeyUser = publicKeyUserRepository.findByUserHandle(credentialRecord.userEntityUserId) ?: return
        val userId = publicKeyUser.userId

        repository.save(
            PublicKeyCredential(
                id = credentialRecord.credentialId,
                publicKeyUserId = credentialRecord.userEntityUserId,
                userId = userId,
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
                lastUsedAt = credentialRecord.lastUsed
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
    val userRepository: UserRepository,
    private val repository: PublicKeyUserRepository
) : PublicKeyCredentialUserEntityRepository {
    override fun findById(id: Bytes): PublicKeyCredentialUserEntity? {
        return repository.findByUserHandle(id)
    }

    override fun findByUsername(username: String): PublicKeyCredentialUserEntity? {
        var record = repository.findByName(username)
        if (record == null) {
            val user = userRepository.findByUserName(username) ?: return null
            record = PublicKeyUser(
                userId = user.id,
                userHandle = stableWebAuthnUserHandle(user.id),
                name = username,
                displayName = user.fullName,
                id = ObjectId()
            )
            repository.save(record)
        }
        return record
    }

    override fun save(userEntity: PublicKeyCredentialUserEntity) {
        // This repository is only used by the WebAuthn flow; we still enforce that the username maps to a real user.
        val user = userRepository.findByUserName(userEntity.name) ?: return
        repository.save(
            PublicKeyUser(
                userId = user.id,
                userHandle = userEntity.id,
                name = userEntity.name,
                displayName = userEntity.displayName,
                id = ObjectId()
            )
        )
    }

    override fun delete(id: Bytes) {
        val existing = repository.findByUserHandle(id) ?: return
        repository.deleteById(existing.id)
    }

}

@Configuration
class WebAuthnConfig(
    private val appConfigService: AppConfigService,
) {
    @Bean
    fun requestOptionsRepository(): PublicKeyCredentialRequestOptionsRepository {
        return HttpSessionPublicKeyCredentialRequestOptionsRepository()
    }

    @Bean
    fun publicKeyCredentialCreationOptionsRepository(): HttpSessionPublicKeyCredentialCreationOptionsRepository {
        return HttpSessionPublicKeyCredentialCreationOptionsRepository()
    }

    @Bean
    fun relyingPartyOperations(
        userCredentialRepository: UserCredentialRepository,
        userEntityRepository: PublicKeyCredentialUserEntityRepository
    ): WebAuthnRelyingPartyOperations {
        return Webauthn4JRelyingPartyOperations(
            userEntityRepository,
            userCredentialRepository,
            PublicKeyCredentialRpEntity.builder()
                .id(appConfigService.rpId() ?: "localhost")
                .name(appConfigService.getConfig().name ?: "komposeauth")
                .build(),
            appConfigService.webauthnAllowedOrigins()
        )
    }

}
