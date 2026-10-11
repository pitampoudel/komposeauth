package pitampoudel.komposeauth.webauthn.config

import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.web.webauthn.api.*
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest
import org.springframework.security.web.webauthn.management.PublicKeyCredentialRequestOptionsRequest
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.user.repository.UserRepository
import pitampoudel.komposeauth.webauthn.entity.PublicKeyCredential
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser
import pitampoudel.komposeauth.webauthn.repository.PublicKeyCredentialRepository
import pitampoudel.komposeauth.webauthn.repository.PublicKeyUserRepository
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
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
        // Saved silently as nothing, a passkey whose handle maps to no account looked registered and
        // could never sign anybody in.
        val publicKeyUser = publicKeyUserRepository.findByUserHandle(credentialRecord.userEntityUserId)
            ?: throw IllegalStateException("No account holds the passkey's user handle")
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
                createdAt = credentialRecord.created ?: Instant.now(),
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
        return repository.findByUserHandle(id)?.asUserEntity()
    }

    override fun findByUsername(username: String): PublicKeyCredentialUserEntity? {
        repository.findByName(username)?.let { return it.asUserEntity() }
        val user = userRepository.findByUserName(username) ?: return null
        // By account rather than by name: the same account reached under another of its names
        // (its id, its email) already has a handle, and a second record would collide with it.
        repository.findByUserId(user.id)?.let { return it.asUserEntity() }
        return repository.save(
            PublicKeyUser(
                userId = user.id,
                userHandle = stableWebAuthnUserHandle(user.id),
                name = username,
                displayName = user.fullName,
                id = ObjectId()
            )
        ).asUserEntity()
    }

    /**
     * What goes into the options a client is handed. The stored record also has an `id` of its own,
     * its ObjectId, which is what the client was sent as `user.id` in place of the handle; and the
     * WebAuthn API refuses a user without a `displayName`, though an empty one is allowed.
     */
    private fun PublicKeyUser.asUserEntity(): PublicKeyCredentialUserEntity =
        ImmutablePublicKeyCredentialUserEntity.builder()
            .id(userHandle)
            .name(name)
            .displayName(displayName.orEmpty())
            .build()

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
    private val appConfigService: AppConfigService
) {
    @Bean
    fun relyingPartyOperations(
        userCredentialRepository: UserCredentialRepository,
        userEntityRepository: PublicKeyCredentialUserEntityRepository
    ): WebAuthnRelyingPartyOperations =
        ConfiguredRelyingPartyOperations(appConfigService, userEntityRepository, userCredentialRepository)

}

/**
 * The relying party's id, name and origins are set on the config page of a running server, so the
 * operations are rebuilt when they change instead of being fixed at boot, where a deployment
 * configured after startup offered passkeys for `localhost` until it was restarted.
 */
private class ConfiguredRelyingPartyOperations(
    private val appConfigService: AppConfigService,
    private val userEntities: PublicKeyCredentialUserEntityRepository,
    private val credentials: UserCredentialRepository
) : WebAuthnRelyingPartyOperations {
    private data class Settings(val rpId: String, val name: String, val origins: Set<String>)

    private val current = AtomicReference<Pair<Settings, WebAuthnRelyingPartyOperations>?>(null)

    private fun operations(): WebAuthnRelyingPartyOperations {
        val settings = Settings(
            rpId = appConfigService.rpId() ?: "localhost",
            name = appConfigService.getConfig().name ?: "komposeauth",
            origins = appConfigService.webauthnAllowedOrigins()
        )
        current.get()?.takeIf { it.first == settings }?.let { return it.second }
        val built = Webauthn4JRelyingPartyOperations(
            userEntities,
            credentials,
            PublicKeyCredentialRpEntity.builder().id(settings.rpId).name(settings.name).build(),
            settings.origins
        )
        current.set(settings to built)
        return built
    }

    override fun createPublicKeyCredentialCreationOptions(request: PublicKeyCredentialCreationOptionsRequest) =
        operations().createPublicKeyCredentialCreationOptions(request)

    override fun registerCredential(request: RelyingPartyRegistrationRequest) =
        operations().registerCredential(request)

    override fun createCredentialRequestOptions(request: PublicKeyCredentialRequestOptionsRequest) =
        operations().createCredentialRequestOptions(request)

    override fun authenticate(request: RelyingPartyAuthenticationRequest) =
        operations().authenticate(request)
}
